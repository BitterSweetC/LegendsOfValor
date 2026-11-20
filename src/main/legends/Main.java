package legends;

import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

import legends.config.ConfigLoader;
import legends.config.GameConfig;
import legends.controller.GameController;
import legends.controller.LvGameController;
import legends.controller.MhGameController;

public class Main {
    public static void main(String[] args) {
        try {
            Path data = Paths.get("data");
            long seed = System.nanoTime();
            boolean colorEnabled = true;
            for(int i = 0; i < args.length; i++) {
                if(args[i].equals("--data") && i + 1 < args.length)
                    data = Paths.get(args[++i]);
                else if(args[i].equals("--seed") && i + 1 < args.length)
                    seed = Long.parseLong(args[++i]);
                else if(args[i].equals("--no-color"))
                    colorEnabled = false;
                else
                    throw new IllegalArgumentException("Usage: Main [--data directory] [--seed integer] [--no-color]");
            }
            GameConfig config = new ConfigLoader().load(data);
            runSelector(config, seed, colorEnabled, new Scanner(System.in));
        }
        catch(IOException e) {
            System.err.println("Cannot read game data: " + e.getMessage());
            System.exit(2);
        }
        catch(IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
        catch(UncheckedIOException e) {
            System.err.println("Console I/O failed: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void runSelector(GameConfig config, long seed, boolean colorEnabled, Scanner input) {
        while(true) {
            System.out.println("\n=== Select a Game to Play ===");
            System.out.println("1. Monsters and Heroes");
            System.out.println("2. Legends of Valor");
            System.out.println("0. Exit");
            System.out.println("\033[36mSelect a game > \033[0m");

            if(!input.hasNextLine()) {
                if(input.ioException() != null)
                    throw new UncheckedIOException(input.ioException());
                return;
            }

            String choice = input.nextLine().trim();
            GameController controller = null;

            switch (choice) {
                case "0":
                    return;
                case "1":
                    controller = new MhGameController(config, seed, colorEnabled, input);
                    break;
                case "2":
                    controller = new LvGameController(config, seed, colorEnabled, input);
                    break;
                default:
                    System.out.println("Enter 1, 2, or 0.");
                    break;
            }

            if (controller != null) {
                controller.run();
            }
        }
    }
}
