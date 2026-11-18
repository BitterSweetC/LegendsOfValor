package legends.controller;

import java.util.Scanner;
import legends.config.GameConfig;

/**
 * Abstract Game Controller Class.
 * Holds the common functionality and state for each concrete game controller.
 */
public abstract class GameController {

    // Shared terminal colors are protected so concrete controllers can use them.
    protected static final String RESET = "\u001B[0m";
    protected static final String RED = "\u001B[31m";
    protected static final String GREEN = "\u001B[32m";
    protected static final String YELLOW = "\u001B[33m";
    protected static final String CYAN = "\u001B[36m";

    private final Scanner input;
    private final GameConfig config;
    private final long seed;
    private final boolean colorEnabled;

    public GameController(GameConfig config, long seed,
                          boolean colorEnabled, Scanner input) {
        if (config == null || input == null) {
            throw new IllegalArgumentException("Config and Scanner required.");
        }

        this.config = config;
        this.seed = seed;
        this.colorEnabled = colorEnabled;
        this.input = input;
    }

    protected final Scanner getInput() {
        return input;
    }

    protected final GameConfig getConfig() {
        return config;
    }

    protected final long getSeed() {
        return seed;
    }

    protected final boolean isColorEnabled() {
        return colorEnabled;
    }

    public abstract void run();

    // Shared display functionality

    protected final String colored(String message, String color) {
        return colorEnabled ? color + message + RESET : message;
    }

    protected final void printError(String message) {
        System.out.println(colored(message, RED));
    }

    protected final void printSuccess(String message) {
        System.out.println(colored(message, GREEN));
    }

    protected final void printInfo(String message) {
        System.out.println(colored(message, CYAN));
    }

    // Shared input functionality

    protected final String readLine(String prompt) {
        System.out.print(colored(prompt, CYAN));

        if (!input.hasNextLine()) {
            throw new IllegalStateException("Input stream closed.");
        }

        return input.nextLine().trim();
    }

    protected final int readNumber(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);

            try {
                int selection = Integer.parseInt(value);

                if (selection >= min && selection <= max) {
                    return selection;
                }

                printError("Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Invalid input. Please enter a number.");
            }
        }
    }
}
