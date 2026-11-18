package legends.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

import legends.config.GameConfig;
import legends.entity.template.HeroTto;

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
            throw new NoSuchElementException("Input stream closed.");
        }

        return input.nextLine().trim();
    }

    protected <T> T choose(String prompt, List<T> options) {
        return choose(prompt, options, Object::toString);
    }

    protected HeroTto chooseHero(String prompt, List<HeroTto> heroes) {
        List<String> classes = new ArrayList<>();
        for(HeroTto hero : heroes)
            if(!classes.contains(hero.getHeroClass()))
                classes.add(hero.getHeroClass());
        String heroClass = choose("Select class", classes);
        if(heroClass == null)
            return null;
        List<HeroTto> candidates = new ArrayList<>();
        for(HeroTto hero : heroes)
            if(hero.getHeroClass().equals(heroClass))
                candidates.add(hero);
        return choose(prompt, candidates, this::heroDescription);
    }

    protected <T> T choose(String prompt, List<T> options, Function<T, String> description) {
        if(options.isEmpty()) {
            System.out.println(prompt + ": nothing available to select. Returning to the previous menu.");
            return null;
        }
        for(int i = 0; i < options.size(); i++)
            System.out.println((i + 1) + ". " + description.apply(options.get(i)));
        int selection = readNumber(prompt + " (0 cancel)", 0, options.size());
        return selection == 0 ? null : options.get(selection - 1);
    }

    // Default text for the selection menu. A game overrides this if it shows different details.
    protected String heroDescription(HeroTto hero) {
        return hero.getName() + " [" + hero.getHeroClass() + ", HP " + hero.getHp() + ", MP "
                + hero.getMp() + ", STR " + hero.getStrength() + ", DEX " + hero.getDexterity()
                + ", AGI " + hero.getAgility() + ", Gold " + hero.getInitialGold()
                + ", EXP " + hero.getInitialExp() + "]";
    }

    // Per-game global commands (help, map, quit...). Return true if the input was handled.
    // This should be overriden
    protected boolean handleCommand(String value) {
        return false;
    }

    protected final int readNumber(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt + " > ");
            if (handleCommand(value))
                continue;
            try {
                int selection = Integer.parseInt(value);
                if (selection >= min && selection <= max)
                    return selection;
                printError("Enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                printError("Invalid input. Please enter a number.");
            }
        }
    }
}