package com.dtheng.pulseberry;

import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniel Thengvall <fender5289@gmail.com>
 */
@Slf4j
public class PulseBerry {

    private static final GpioController controller = GpioFactory.getInstance();

    private static final List<GpioPinDigitalOutput> LIGHTS = Arrays.asList(
            controller.provisionDigitalOutputPin(RaspiPin.GPIO_00),
            controller.provisionDigitalOutputPin(RaspiPin.GPIO_01),
            controller.provisionDigitalOutputPin(RaspiPin.GPIO_02),
            controller.provisionDigitalOutputPin(RaspiPin.GPIO_07)
    );

    private static final List<GpioPinDigitalInput> BUTTONS = Arrays.asList(
            controller.provisionDigitalInputPin(RaspiPin.GPIO_04),
            controller.provisionDigitalInputPin(RaspiPin.GPIO_05),
            controller.provisionDigitalInputPin(RaspiPin.GPIO_06),
            controller.provisionDigitalInputPin(RaspiPin.GPIO_03)
    );

    private static Map<String, Date> lastPresses = new HashMap<>();
    private static Map<String, Integer> pressCounts = new HashMap<>();
    private static Map<String, Subscription> pulseStreams = new HashMap<>();

    public static void main(String args[]) {

        log.info("Starting...");

        if (LIGHTS.size() != BUTTONS.size())
            throw new RuntimeException("List of lights must be equal in length to list of buttons");

        for (int i = 0; i < BUTTONS.size(); i++) {
            log.info("Configuring button/light pair #{}", i);
            LIGHTS.get(i).setState(false);
            BUTTONS.get(i)
                    .addListener(new PinListener(i));
        }

        try {
            Thread.sleep(1000 * 60 * 60 * 24 * 24);
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    private static void updatePulseSpeed(int buttonId, long speedMs) {
        String key = "buttonId"+ buttonId;
        if (pulseStreams.containsKey(key))
            pulseStreams.get(key).unsubscribe();
        pulseStreams.put(key, Observable.interval(0, speedMs, TimeUnit.MILLISECONDS, Schedulers.io())
            .flatMap(Void -> pulse(LIGHTS.get(buttonId), (int) speedMs))
                .subscribe());
    }

    private static Observable<Void> pulse(GpioPinDigitalOutput digitalOutput, int speedInMs) {
        return Observable.defer(() -> {
                digitalOutput.setState(true);
                return Observable.timer((speedInMs / 3) * 2, TimeUnit.MILLISECONDS)
                    .doOnNext(Void -> digitalOutput.setState(false))
                    .delay(speedInMs / 3, TimeUnit.MILLISECONDS);
            })
            .ignoreElements().cast(Void.class);
    }

    @AllArgsConstructor
    private static class PinListener implements GpioPinListenerDigital {

        private int buttonId;

        @Override
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
            if (event.getState() == PinState.HIGH) {
                String key = "buttonId"+ buttonId;
                if ( ! pressCounts.containsKey(key))
                    pressCounts.put(key, 1);
                else
                    pressCounts.put(key, pressCounts.get(key) +1);
                if (pressCounts.get(key) % 2 == 0) {
                    log.debug("Updating pulse speed for #{}", buttonId);
                    long timeElapsed = new Date().getTime() - lastPresses.get(key).getTime();
                    updatePulseSpeed(buttonId, timeElapsed);
                } else {
                    log.debug("Listening for #{} second press...", buttonId);
                }
                lastPresses.put(key, new Date());
            }
        }
    }
}