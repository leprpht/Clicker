package com.leprpht.clickerapp.utils;

import java.awt.event.KeyEvent;
import java.awt.*;
import java.awt.event.InputEvent;

/**
 * Executes simple text-based automation scripts for mouse and keyboard actions.
 * <p>
 * Supported commands:
 * <ul>
 *   <li>{@code PRESS <key/button>} - Presses a key or mouse button.</li>
 *   <li>{@code RELEASE <key/button>} - Releases a key or mouse button.</li>
 *   <li>{@code CLICK <key/button>} - Presses and releases a key or mouse button.</li>
 *   <li>{@code MOVE x,y} - Moves the mouse to the given screen coordinates.</li>
 *   <li>{@code WAIT <ms>} - Pauses execution for the given number of milliseconds.</li>
 *   <li>{@code REPEAT <count>} ... {@code END} - Loops the block between REPEAT and END.</li>
 *   <li>{@code HALT} - Stops execution immediately.</li>
 * </ul>
 * <p>
 * Coordinates are absolute screen positions. Key names must match
 * {@link java.awt.event.KeyEvent} VK_* constants (e.g. {@code A}, {@code ENTER}, {@code SPACE}).
 * Mouse buttons: {@code LEFT}, {@code RIGHT}, {@code MIDDLE}.
 */
public class ScriptExecutor {

    /**
     * Indicates whether a script is currently running.
     * This prevents concurrent executions of scripts.
     */
    private volatile boolean running = false;

    /**
     * The {@link Robot} instance used to perform input actions.
     * This is created once per {@code ScriptExecutor} instance.
     */
    private final Robot robot;

    /**
     * Tracks the last known mouse position controlled by the script.
     * <p>
     * This is used to implement the immediate killswitch: before executing
     * each script command, the current system mouse position is compared
     * with {@code mousePosition}. If the positions differ, it indicates
     * that the user has moved the mouse manually, and the script execution
     * is halted to prevent unintended actions.
     * </p>
     *
     * <p>
     * The value is updated whenever the script moves the mouse via the
     * {@link #move(String)} command, ensuring that only user-initiated
     * mouse movements trigger the killswitch.
     * </p>
     *
     * <p>
     * Initially {@code null} until execution starts and the first mouse
     * position is recorded.
     * </p>
     */
    private Point mousePosition = null;

    private enum InputType { MOUSE, KEYBOARD }

    /**
     * Represents a parsed input action (mouse or keyboard) along with its native code.
     */
    private static class InputAction {
        InputType type;
        int code;
        InputAction(InputType type, int code) {
            this.type = type;
            this.code = code;
        }
    }

    /**
     * Creates a new {@code ScriptExecutor} with its own {@link Robot}.
     *
     * @throws RuntimeException if the {@link Robot} cannot be created.
     */
    public ScriptExecutor() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Executes the given script string.
     * Each line is parsed and executed sequentially until the script ends or is halted.
     *
     * @param script Script text to execute.
     */
    public void execute(String script) {
        if (running) return;
        running = true;

        String[] lines = script.split("\\n");
        Point current = MouseInfo.getPointerInfo().getLocation();
        mousePosition = new Point(current.x, current.y);
        executeLines(lines, 0, lines.length);
        running = false;
    }

    /**
     * Executes a block of lines between the given indices.
     *
     * @param lines Array of script lines.
     * @param start Start index (inclusive).
     * @param end End index (exclusive).
     */
    private void executeLines(String[] lines, int start, int end) {
        for (int i = start; i < end; i++) {
            if (hasMouseMoved()) {
                halt();
            }
            if (!running) return;

            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" ", 2);
            String command = parts[0];
            String args = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "PRESS" -> press(args);
                case "CLICK" -> click(args);
                case "RELEASE" -> release(args);
                case "WAIT" -> wait(args);
                case "REPEAT" -> i = repeat(args, lines, i + 1, end);
                case "MOVE" -> move(args);
                case "HALT" -> {
                    halt();
                    return;
                }
                case "END" -> {
                    return;
                }
            }
        }
    }

    /**
     * Repeats a block of lines a given number of times.
     * @param countStr String representation of the repeat count.
     * @param lines Array of script lines.
     * @param start Start the index of the block (after REPEAT).
     * @param end End index of the outer block.
     * @return Index to resume execution after the repeated block.
     */
    private int repeat(String countStr, String[] lines, int start, int end) {
        int count;
        try {
            count = Integer.parseInt(countStr.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid repeat count: " + countStr);
            return start;
        }

        int blockEnd = findMatchingEnd(lines, start, end);
        for (int i = 0; i < count && running; i++) {
            executeLines(lines, start, blockEnd);
        }

        return blockEnd;
    }

    /**
     * Finds the matching {@code END} for a {@code REPEAT} command.
     *
     * @param lines Array of script lines.
     * @param start Start the index of the search.
     * @param end   End index of the search.
     * @return Index of the matching END line.
     * @throws IllegalStateException if no matching END is found.
     */
    private int findMatchingEnd(String[] lines, int start, int end) {
        int depth = 0;
        for (int i = start; i < end; i++) {
            String line = lines[i].trim();
            if (line.startsWith("REPEAT")) depth++;
            else if (line.equals("END")) {
                if (depth == 0) return i;
                else depth--;
            }
        }
        throw new IllegalStateException("No matching END found for REPEAT starting at line " + start);
    }

    /**
     * Stops the current script execution immediately.
     */
    public void halt() {
        running = false;
    }

    /**
     * Presses a key or mouse button without releasing it.
     *
     * @param key Name of the key or button.
     */
    private void press(String key) {
        try {
            InputAction action = parseKeyOrButton(key);
            if (action.type == InputType.MOUSE) {
                robot.mousePress(action.code);
            } else {
                robot.keyPress(action.code);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Skipping invalid key/button: " + key);
        }
    }

    /**
     * Presses and releases a key or mouse button.
     *
     * @param key Name of the key or button.
     */
    private void click(String key) {
        try {
            InputAction action = parseKeyOrButton(key);
            if (action.type == InputType.MOUSE) {
                robot.mousePress(action.code);
                robot.mouseRelease(action.code);
            } else {
                robot.keyPress(action.code);
                robot.keyRelease(action.code);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Skipping invalid key/button: " + key);
        }
    }

    /**
     * Releases a key or mouse button.
     *
     * @param key Name of the key or button.
     */
    private void release(String key) {
        try {
            InputAction action = parseKeyOrButton(key);
            if (action.type == InputType.MOUSE) {
                robot.mouseRelease(action.code);
            } else {
                robot.keyRelease(action.code);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Skipping invalid key/button: " + key);
        }
    }

    /**
     * Parses a key or mouse button name into an {@link InputAction}.
     *
     * @param key Name of the key or button.
     * @return Parsed {@link InputAction}.
     * @throws IllegalArgumentException if the key/button is not recognized.
     */
    private InputAction parseKeyOrButton(String key) {
        key = key.trim().toUpperCase();
        switch (key) {
            case "LEFT" -> {
                return new InputAction(InputType.MOUSE, InputEvent.BUTTON1_DOWN_MASK);
            }
            case "RIGHT" -> {
                return new InputAction(InputType.MOUSE, InputEvent.BUTTON3_DOWN_MASK);
            }
            case "MIDDLE" -> {
                return new InputAction(InputType.MOUSE, InputEvent.BUTTON2_DOWN_MASK);
            }
            default -> {
                try {
                    int code = (int) KeyEvent.class.getField("VK_" + key).get(null);
                    return new InputAction(InputType.KEYBOARD, code);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid key/button: " + key);
                }
            }
        }
    }

    /**
     * Moves the mouse to the specified screen coordinates.
     *
     * @param position Comma-separated x,y coordinates (e.g. "100,200").
     */
    private void move(String position) {
        try {
            String[] parts = position.trim().split(",");
            if (parts.length != 2) {
                System.err.println("Invalid MOVE format. Use: MOVE x,y");
                return;
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());

            robot.mouseMove(x, y);

            Point current;
            int threshold = 2;
            int attempts = 0;
            do {
                Thread.sleep(5);
                current = MouseInfo.getPointerInfo().getLocation();
                attempts++;
                if (attempts > 50) break;
            } while (Math.abs(current.x - x) > threshold || Math.abs(current.y - y) > threshold);

            mousePosition = new Point(current.x, current.y);
        } catch (NumberFormatException e) {
            System.err.println("Invalid MOVE coordinates: " + position);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Mouse move interrupted.");
        } catch (Exception e) {
            System.err.println("Error moving mouse: " + e.getMessage());
        }
    }

    /**
     * Waits for the specified number of milliseconds before continuing execution.
     *
     * @param ms Milliseconds to wait.
     */
    private void wait(String ms) {
        try {
            int milliseconds = Integer.parseInt(ms);
            Thread.sleep(milliseconds);
        } catch (NumberFormatException | InterruptedException e) {
            System.err.println("Invalid wait time: " + ms);
        }
    }

    /**
     * Checks if the user has moved the mouse manually.
     * If the mouse position has changed since the last script command,
     * it indicates that the user has moved the mouse, and the script
     * execution should be halted immediately.
     * The {@code threshold} is set to 2 pixels add small tolerance.
     *
     * @return {@code true} if the mouse has moved, {@code false} otherwise.
     */
    private boolean hasMouseMoved() {
        Point current = MouseInfo.getPointerInfo().getLocation();
        int dx = Math.abs(current.x - mousePosition.x);
        int dy = Math.abs(current.y - mousePosition.y);
        int threshold = 2;
        return dx > threshold || dy > threshold;
    }
}
