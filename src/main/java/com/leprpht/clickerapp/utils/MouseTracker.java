package com.leprpht.clickerapp.utils;

import com.leprpht.clickerapp.controllers.MainController;
import java.awt.*;

/**
 * Utility class for tracking the mouse cursor position in real time.
 */
public class MouseTracker {

    /**
     * Flag to indicate whether the mouse tracking thread is running.
     */
    private static volatile boolean running = false;

    /**
     * The thread that performs mouse tracking.
     * This is used to stop the thread gracefully when needed.
     */
    private static Thread trackingThread;

    /**
     * Starts a background thread that continuously polls the current mouse position
     * and updates the UI via the provided controller.
     *
     * @param controller the {@link MainController} that will receive mouse coordinate updates
     */
    public static void Track(MainController controller) {
        if (running) return;
        running = true;
        trackingThread = new Thread(() -> {
            while (running) {
                Point point = MouseInfo.getPointerInfo().getLocation();
                int x = point.x;
                int y = point.y;

                controller.mousePosition(x, y);

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        trackingThread.setDaemon(true);
        trackingThread.start();
    }

    /**
     * Stops the mouse tracking thread.
     */
    public static void stop() {
        running = false;
        if (trackingThread != null) {
            trackingThread.interrupt();
            trackingThread = null;
        }
    }
}
