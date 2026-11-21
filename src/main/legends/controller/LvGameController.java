package legends.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.*;

import legends.config.GameConfig;
import legends.entity.*;
import legends.entity.template.HeroTto;
import legends.enums.GameState;
import legends.factory.HeroFactory;
import legends.factory.MonsterWaveGenerator;
import legends.factory.VWorldGenerator;
import legends.service.CombatService;
import legends.service.DamageCalculatorService;
import legends.service.MarketService;
import legends.service.impl.CombatServiceImpl;
import legends.service.impl.DamageCalculatorServiceImpl;
import legends.service.impl.MarketServiceImpl;

/**
 * Controller for Legends of Valor.
 * Runs setup, the hero phase (one action per living hero), the monster phase,
 * round end (recovery, respawns, waves) and the end-of-game screen.
 */
public final class LvGameController extends GameController {
    // Game Constants: todo verify for fairness and smooth gameplay
    private static final double RECOVERY = 0.10; // HP and MP regained per round by living heroes
    private static final int WAVE_INTERVAL = 8; // a new wave after every 8 completed rounds
    private static final int POTION_ROUNDS = 3; // potion-buff lifetime in Valor
    private static final int RESTOCK_EVERY_WAVES = 3; // market refresh policy

    private LvGameSession session;
    private ValorWorld world;
    private Random random;
    private MarketService markets;
    private CombatService combat;
    private MonsterWaveGenerator waves;
    private Market market; // shared by the three heroes' Nexus spaces
    private int wavesSpawned;
    private final List<QueuedSpawn> queued = new ArrayList<>();
    private HeroSlot announced; // hero whose turn banner was already printed
    private SessionStatistics statistics;

    public LvGameController(GameConfig config, long seed, boolean colorEnabled, Scanner input) {
        super(config, seed, colorEnabled, input);
    }

    /** Main Game Loop: call this method in Main to start the game
     * Starts the game session and runs the heroes' turn first
     * Next it runs the monsters' turn until the game is over
     * Prompts the user to play again if they'd like
     * If not exits back to the main menu.
     */
    @Override
    public void run() {
        System.out.println("Legends of Valor | seed " + getSeed());
        help();
        try {
            boolean again = true;
            while(again) {
                setup();
                while(!session.isOver()) {
                    try {
                        if(session.getState() == GameState.HERO_PHASE)
                            heroPhase();
                        else
                            monsterPhase();
                    }
                    catch(IllegalArgumentException e) {
                        printError("Invalid command: " + e.getMessage());
                    }
                }
                if(session.didHeroesWin())
                    printSuccess("Victory :D! Your heroes reached the monsters' Nexus.");
                else
                    printError("Defeat D:!. A monster reached your Nexus.");
                again = askRestart();
            }
        }
        catch(QuitGameException e) {
            // fall through control returns to the selector
        }
        catch(NoSuchElementException e) {
            if(getInput().ioException() != null)
                throw new UncheckedIOException(getInput().ioException());
        }

        System.out.println("Returning to the game menu.");
    }

    // Prompts the user to restart the game
    private boolean askRestart() {
        while(true) {
            String choice = readLine("Game over. R restart, X exit > ").toUpperCase();
            if(choice.equals("R"))
                return true;
            if(choice.equals("X"))
                return false;
            if(!handleCommand(choice))
                printError("Enter R or X.");
        }
    }

    /** Game Setup:
     * Start the Game Session
     * Generate the World
     * Generate the Market
     * Spawn the Heroes
     */
    private void setup() {
        session = null;
        world = null;
        market = null;
        wavesSpawned = 0;
        queued.clear();
        announced = null;

        List<HeroTto> available = new ArrayList<>(getConfig().getHeroTtos()); // heroes user hasn't picked yet
        List<Hero> heroes = new ArrayList<>(); // heroes the user has picked
        int[] lanes = new int[LvGameSession.LANES];
        boolean[] taken = new boolean[LvGameSession.LANES]; // occupied lanes

        // prompt the user to select an available hero until they selected enough for each lane
        while(heroes.size() < LvGameSession.LANES) {
            HeroTto definition = chooseHero("Select hero " + (heroes.size() + 1), available);
            if(definition == null)
                continue;
            int lane = -1;

            // prompt the user to pick the lane for each hero
            while(lane < 0) {
                int pick = readNumber("Lane for " + definition.getName() + " (1 Top Lane, 2 Middle Lane, 3 Bottom Lane)", 1, 3) - 1;
                if(taken[pick])
                    printError("Lane " + pick + " already has a hero, choose a different one.");
                else
                    lane = pick;
            }
            taken[lane] = true;
            lanes[heroes.size()] = lane;
            heroes.add(HeroFactory.create(definition, getConfig()));
            available.remove(definition);
        }

        // Creating the session statistics and registering chosen heroes so we can use the statistics later
        random = new Random(getSeed());
        statistics = new SessionStatistics();
        markets = new MarketServiceImpl(getConfig(), random, statistics);
        for(Hero hero : heroes) {
            statistics.register(hero);
            markets.starter(hero);
        }

        // generate the world and start the game session
        // creates the hero slots, generates the lanes, and different space types
        world = new VWorldGenerator().generate(random);
        session = new LvGameSession(world, heroes, lanes);

        // places the hero slot in order so board labels are H1-H3
        for(HeroSlot slot : session.getSlots())
            if(!world.placeHero(slot.getHero(), slot.getHome()))
                throw new IllegalStateException("Could not place " + slot.getHero().getName());

        combat = new CombatServiceImpl(random);
        waves = new MonsterWaveGenerator();
        market = createMarket(); // shared market
        spawnWave(); // spawn the initial monsters
    }

    // creates a shared market that all heroes use at their nexus
    private Market createMarket() {
        Market created = markets.generate();
        markets.distinguish(created, new HashSet<String>());
        return created;
    }

    // main method for the heroes actions
    private void heroPhase() {
        HeroSlot slot = session.nextToAct(); // get the current hero whose turn it is
        if(slot == null) { // every living hero has acted or they all died
            session.beginMonsterPhase(); // run the monsters turn if they're alive
            return;
        }
        Hero hero = slot.getHero();
        if(announced != slot) {
            announceTurn(slot); // announce the current heroes' turn
            announced = slot;
        }

        String command = readLine(world.getLabel(hero) + " action > ").toUpperCase(); // get the user command
        if(handleCommand(command))
            return; // non-turn consuming command entered, same hero is prompted again

        boolean acted; // whether or not the command consumes a turn
        switch(command) {
            // movement commands
            case "W" : acted = move(hero, -1, 0); break;
            case "S" : acted = move(hero, 1, 0); break;
            case "A" : acted = move(hero, 0, -1); break;
            case "D" : acted = move(hero, 0, 1); break;

            // combat commands
            case "F" : acted = attack(hero); break;
            case "C" : acted = cast(hero); break;
            case "U" : acted = usePotion(hero); break;
            case "E" : acted = equip(hero); break;

            // other commands
            case "T" : acted = teleport(hero); break;
            case "R" : acted = recall(slot); break;
            case "O" : acted = clearObstacle(hero); break;
            case "B" : market(hero); acted = false; break; // non-turn consuming
            case "X" : acted = true; break; // skip/give up turn
            default :
                printError(command + "is an unknown command. Type H for help.");
                acted = false;
        }

        // if we get here the hero successfully completed a turn consuming action
        // reset announced hero so we can announce the next heroes turn
        if(acted) {
            session.markActed(hero);
            announced = null;
            checkNexus(); // check if monsters or heroes reached a nexus if so the game ends
        }
    }

    // helper to print the current heroes turn
    private void announceTurn(HeroSlot slot) {
        Hero hero = slot.getHero();
        DamageCalculatorService calculator = new DamageCalculatorServiceImpl();
        System.out.println(world.render(isColorEnabled()));
        System.out.println(colored("=== Round " + session.getRound() + " | " + world.getLabel(hero) + " "
                + hero.getName() + " (Lv" + hero.getLevel() + " " + hero.getHeroClass() + ") | HP "
                + hero.getHp() + "/" + hero.getMaxHp() + " | MP " + hero.getMp() + "/" + hero.getMaxMp()
                + " | " + at(world.getPosition(hero)) + " " + world.getTile(world.getPosition(hero)) + " ===", CYAN));
        System.out.println("Attack power " + Math.round(calculator.getPhysical(hero)) + " | DEX "
                + Math.round(hero.getStat("DEX")) + " | Dodge " + Math.round(100 * calculator.getDodge(hero, 0)) + "%");
        System.out.println("W/A/S/D move, F attack, C spell, U potion, E equip, T teleport, R recall, "
                + "O clear obstacle, B market, X pass, H help");
    }

    // helper to verify the move is valid if it is then move the hero to the new spot
    private boolean move(Hero hero, int rowChange, int colChange) {
        if(!world.moveHero(hero, rowChange, colChange)) {
            printError("You cannot move there: edge, wall, obstacle, another hero, or a monster you must defeat first.");
            return false;
        }
        return true;
    }

    private boolean attack(Hero hero) {
        // get the monsters in range that the hero can attack
        List<Monster> targets = world.getAttackableMonsters(hero);
        if(targets.isEmpty()) { // no monsters in range, exit this method
            printError("No monster in range.");
            return false;
        }
        // allow the hero to pick which monster in range they want to attack
        Monster target = choose("Target", targets, this::describeMonster);
        if(target == null)
            return false;
        logs(combat.attack(hero, target)); // attack the target monster and add it to the logs to be printed
        resolveKill(target); // check and handle if the monster fainted
        return true;
    }

    private boolean cast(Hero hero) {
        // only can cast a spell on monsters in an attackable range
        List<Monster> targets = world.getAttackableMonsters(hero);
        if(targets.isEmpty()) {
            printError("No monster in range.");
            return false;
        }
        // choose which spell to cast
        ItemInstance spell = choose("Spell", items(hero, "spells"));
        if(spell == null)
            return false;
        // choose which monster to cast the spell on
        Monster target = choose("Target", targets, this::describeMonster);
        if(target == null)
            return false;
        // cast the spell and add the outcome to the logs to be printed
        logs(combat.cast(hero, spell, target));
        resolveKill(target); // check and handle if the monster fainted
        return true;
    }

    private boolean usePotion(Hero hero) {
        // choose the potion to use
        ItemInstance potion = choose("Potion", items(hero, "potions"));
        if(potion == null)
            return false;
        // apply the potion effects to the hero
        // they last for duration = POTION_ROUNDS
        // add the result to the logs to be printed
        logs(combat.potion(hero, potion, POTION_ROUNDS));
        return true;
    }

    private boolean equip(Hero hero) {
        List<ItemInstance> list = new ArrayList<>();
        // get all items that still have durability
        for(ItemInstance item : hero.getInventory().getItems())
            if(item.isDurable())
                list.add(item);
        System.out.println(hero.getEquipment());
        // prompt user to pick an item from their inventory
        ItemInstance item = choose("Equipment", list);
        if(item == null)
            return false;

        String mode;
        while(true) {
            // ask how many hands to use for the item
            mode = readLine("SINGLE main hand / BOTH both hands / OFF off hand; U unequip, 0 cancel > ").toUpperCase();
            if(!handleCommand(mode))
                break;
        }
        if(mode.equals("0"))
            return false;
        if(mode.equals("U")) { // unequip item
            if(!hero.getEquipment().isEquipped(item))
                throw new IllegalArgumentException("Item is not equipped");
            logs(combat.unequip(hero, item));
        }
        else // equip item
            logs(combat.equip(hero, item, mode));
        return true;
    }

    private boolean teleport(Hero hero) {
        List<Hero> mates = new ArrayList<>(); // fellow heroes we can teleport to
        for(Hero mate : session.getLivingHeroes()) // they must be alive in order to teleport to them
            if(mate != hero && !world.getTeleportTargets(hero, mate).isEmpty())
                mates.add(mate);
        if(mates.isEmpty()) {
            printError("No teleport destination is available (needs a living hero in another lane).");
            return false;
        }
        // choose which valid hero you want to teleport to
        Hero mate = choose("Teleport next to", mates,
                m -> world.getLabel(m) + " " + m.getName() + " at " + at(world.getPosition(m)));
        if(mate == null)
            return false;
        // choose which position you want to teleport to
        Position target = choose("Destination", world.getTeleportTargets(hero, mate),
                p -> at(p) + " (" + world.getTile(p) + ")");
        if(target == null)
            return false;
        if(!world.teleportHero(hero, mate, target)) { // teleport the hero to the target position
            printError("Teleport failed.");
            return false;
        }
        return true;
    }

    private boolean recall(HeroSlot slot) {
        // sends the hero back to their nexus
        // fails when they're already at home or their home space is blocked
        if(!world.recallHero(slot.getHero(), slot.getHome())) {
            printError("Recall failed: already at home, or the home space is occupied by another hero.");
            return false;
        }
        return true;
    }

    private boolean clearObstacle(Hero hero) {
        // obstacles that the hero can remove
        List<Position> obstacles = world.getRemovableObstacles(hero);
        if(obstacles.isEmpty()) {
            printError("No adjacent obstacle to remove.");
            return false;
        }
        // Select the obstacle you want to remove and get it's position
        Position target = choose("Obstacle", obstacles, this::at);
        if(target == null)
            return false;
        // remove the obstacle, it now becomes a plain space
        if(!world.removeObstacle(hero, target)) {
            printError("Cannot remove that obstacle.");
            return false;
        }
        System.out.println("The obstacle at " + at(target) + " is now a plain space.");
        return true;
    }

    private void market(Hero hero) {
        Position position = world.getPosition(hero);
        // verify that the hero is at their associated nexus
        // a hero can only enter the market at their nexus
        if(position == null || !world.isHeroNexus(position.getRow(), position.getCol())) {
            printError("Heroes can only trade at their Nexus. Recall back to your Nexus to enter the market.");
            return;
        }
        while(true) {
            System.out.println("Market | " + hero.getName() + " | Gold " + hero.getGold());
            // prompt the user to perform one of the valid options at the market
            int option = readNumber("1 buy, 2 sell, 3 repair, 4 inventory, 0 leave", 0, 4);
            if(option == 0) // leave the market
                return;
            try {
                long before = hero.getGold(); // previous amount of money
                if(option == 1) { // buy an item
                    // choose the item from the market stock you want to buy
                    ItemInstance item = choose("Buy", market.getStock(), i -> i + " | buy " + markets.getBuyPrice(i)
                            + (i.getLevel() > hero.getLevel() ? " | level too high" : ""));
                    if(item != null) { // if a valid item was chosen then handle the purchase and display the changed gold amount
                        markets.buy(market, hero, item);
                        printSuccess("Purchased " + item.getName() + ". Gold " + before + " -> " + hero.getGold());
                    }
                }
                else if(option == 2) { // sell an item
                    // choose the item from your inventory that you want to sell
                    ItemInstance item = choose("Sell", hero.getInventory().getItems(),
                            i -> i + " | sell " + markets.getSellPrice(i));
                    if(item != null) { // if a valid item was chosen then handle the sell and display the changed gold balance
                        markets.sell(market, hero, item);
                        printSuccess("Sold " + item.getName() + ". Gold " + before + " -> " + hero.getGold());
                    }
                }
                else if(option == 3) { // repair an item
                    // only repairing damaged items which are durable items that have been used
                    // items which are durable but at max durability are not shown since they don't need repair
                    List<ItemInstance> damaged = new ArrayList<>();
                    for(ItemInstance item : hero.getInventory().getItems())
                        if(item.isDurable() && item.getDurability() < item.getMaxDurability())
                            damaged.add(item);
                    // choose which item you want to repair
                    ItemInstance item = choose("Repair", damaged, i -> i + " | repair " + markets.getRepairPrice(i));
                    if(item != null) { // handle the repair, charge the repair amount, show updated gold balance
                        markets.repair(hero, item);
                        printSuccess("Repaired " + item.getName() + ". Gold " + before + " -> " + hero.getGold());
                    }
                }
                else { // display their inventory
                    System.out.println(hero.getName() + " inventory:");
                    for(ItemInstance item : hero.getInventory().getItems())
                        System.out.println("  " + item);
                }
            }
            catch(IllegalArgumentException e) {
                printError("Cannot do that: " + e.getMessage());
            }
        }
    }

    private void monsterPhase() {
        System.out.println(colored("\n=== Monsters' turn ===", RED));
        for(Monster monster : new ArrayList<>(world.getMonsters())) {
            if(!monster.isAlive()) // skip fainted monsters, only alive monsters can act
                continue;
            List<Hero> targets = world.getAttackableHeroes(monster);
            if(!targets.isEmpty()) {
                Hero target = targets.get(0); // In-range heroes are sorted by current HP.
                System.out.println(world.getLabel(monster) + " attacks " + target.getName() + ":");
                logs(combat.monsterAttack(monster, target));
                if(!target.isAlive()) { // handle hero fainting
                    world.removeHero(target);
                    session.scheduleRespawn(target); // they're added to a respawn queue
                    printError(target.getName() + " has fallen and will respawn next round.");
                }
            }
            else if(world.advanceMonster(monster)) // no hero in attack range, monster will try to move forward
                System.out.println(world.getLabel(monster) + " advances to " + at(world.getPosition(monster)) + ".");
            else // no valid moves, the monster will wait until it can advance forward or attack a hero
                System.out.println(world.getLabel(monster) + " is blocked and waits.");
            if(checkNexus()) // handle a victory either monster or hero reached a nexus
                return;
        }
        endRound(); // handle the end of the monster round
    }

    /** Handler for the end of a round i.e. after the Hero and Monster Phase
     * We need to decrement the duration of any effects, remove them if its at 0
     * Recover HP and MANA
     * Try to respawn fainted heroes at their nexus
     * Try to spawn the monsters that couldn’t be placed when their wave started
     * Check if it's time for the next wave of monsters
     * */
    private void endRound() {
        for(Hero hero : session.getLivingHeroes()) {
            hero.endEffects();
            hero.recoverFraction(RECOVERY);
        }
        for(Monster monster : world.getMonsters())
            monster.endEffects();
        session.endRound();

        for(HeroSlot slot : session.getPendingRespawns()) {
            Hero hero = slot.getHero();
            if(world.respawnHero(hero, slot.getHome())) {
                hero.respawn();
                session.completeRespawn(hero);
                printSuccess(hero.getName() + " respawns at " + at(slot.getHome()) + ".");
            }
            else
                printError(hero.getName() + " cannot respawn yet: the home space is occupied.");
        }

        retryQueued(); // try to spawn monsters that are queued
        int completed = session.getRound() - 1;
        if(completed > 0 && completed % WAVE_INTERVAL == 0) {
            printError("A new wave of monsters appears!");
            spawnWave();
        }
        session.beginHeroPhase(); // restart the cycle, now the heroes act
    }

    private void spawnWave() {
        int level = session.getHighestHeroLevel(); // heroes start at level 1, so the first wave is level 1
        List<Monster> wave = waves.generate(getConfig(), random, level, LvGameSession.LANES);
        for(int lane = 0; lane < wave.size(); lane++)
            placeMonster(wave.get(lane), lane); // try to place a monster in each lane

        wavesSpawned++;
        // market refreshes its supply every RESTOCK_EVERY_WAVES
        // market stock is scaled to the highest level hero
        if(wavesSpawned % RESTOCK_EVERY_WAVES == 0 && market != null)
            markets.restock(market, level);
    }

    /** Right Nexus space first, then the left one
     * Add the monster to a Queue to be spawned if both are taken.
     * We will try to spawn them next round.
     */
    private void placeMonster(Monster monster, int lane) {
        Position right = world.getMonsterSpawn(lane);
        Position left = new Position(right.getRow(), right.getCol() - 1);
        if(!world.placeMonster(monster, right) && !world.placeMonster(monster, left))
            queued.add(new QueuedSpawn(monster, lane));
    }

    /** Queue of monsters that couldn't spawn when their wave started
     * Remove them from the queue and try to spawn them again.
     * If we can't spawn them still, requeue them again.
     */
    private void retryQueued() {
        List<QueuedSpawn> waiting = new ArrayList<>(queued);
        queued.clear();
        for(QueuedSpawn spawn : waiting)
            placeMonster(spawn.monster, spawn.lane);
    }

    private void resolveKill(Monster monster) {
        if(monster.isAlive()) // monster is still alive nothing to do
            return;
        printSuccess(world.getLabel(monster) + " " + monster.getName() + " is defeated!");
        long gold = monster.getGold(); // gold and xp associated with defeating this monster
        long exp = monster.getExp();
        for(Hero hero : session.getLivingHeroes()) { // rewarded exactly once, living heroes only
            hero.credit(gold);
            hero.gainExperience(exp);
        }
        System.out.println("Each surviving hero gains " + gold + " gold and " + exp + " EXP.");
        world.removeMonster(monster);
    }

    // main method to check if the game session is over
    // either a hero reached a nexus or a monster reached a nexus
    private boolean checkNexus() {
        if(world.hasHeroReachedEnemyNexus())
            session.finish(true);
        else if(world.hasMonsterReachedHeroNexus())
            session.finish(false);
        return session.isOver();
    }

    // helper to handle the global commands
    // these commands are independent of any menu
    @Override
    protected boolean handleCommand(String value) {
        String command = value.toUpperCase();
        if(command.equals("Q")) { // akss to quit the game
            while(true) {
                String confirm = readLine("Quit this game? Progress is not saved. Y yes, N/0 cancel > ").toUpperCase();
                if(confirm.equals("Y") || confirm.equals("Q"))
                    throw new QuitGameException(); // run() will catch this so the player can return to the game selector menu
                if(confirm.equals("N") || confirm.equals("0"))
                    return true;
                printError("Enter Y or N.");
            }
        }
        if(command.equals("H")) { // display the information about how the game works
            help();
            return true;
        }
        if(command.equals("I")) { // display hero stats, equipment, active status effects
            info();
            return true;
        }
        if(command.equals("P")) { // display the current state of the world
            if(world == null)
                System.out.println("The world is not created yet.");
            else
                System.out.println(world.render(isColorEnabled()));
            return true;
        }
        if(value.isEmpty()) {
            printError("Enter a command; 0 cancels.");
            return true;
        }
        return false;
    }

    // helper to display how the game works
    private void help() {
        System.out.println(
                "============================================ Legends of Valor ============================================\n"
                + "<------------~ COMMANDS ~------------>"
                + "Heroes act first, then monsters. Each living hero takes ONE valid action per round.\n"
                + "W/A/S/D move; F attack; C cast spell; U use potion; E equip/unequip an item; T teleport; R recall;\n"
                + "O clear an adjacent obstacle; X pass turn; B enter market (only allowed while on your Nexus).\n"
                + "Global Commands: H help, I hero info, P show board, Q quit game; 0 cancels any menu.\n"
                + "<------------~ HEROES ~------------>\n"
                + "Attacks and spells reach the current space and its eight neighbors.\n"
                + "A hero cannot move past a monster in its lane without defeating it.\n"
                + "Two heroes cannot occupy the same space.\n"
                + "Bush +10% DEX, Cave +10% AGI, Koulou +10% STR while you stand on it, the bonus ends when you leave.\n"
                + "Living heroes regain 10% of their HP and MANA at the end of each round.\n"
                + "Fallen heroes respawn at their own Nexus at the end of the next round with full HP and MP.\n"
                + "If that space is occupied by another hero, their respawn is pended until the space is free.\n"
                + "<------------~ MONSTERS ~------------>\n"
                + "Each monster will take one of the following actions:\n"
                + "  1. ATTACK: if any hero is in range (the monster's own space or any of the eight adjacent spaces around it).\n"
                + "  2. ADVANCE: if it doesn't attack or is out of range, it moves one space toward your Nexus.\n"
                + "  3. WAIT: if no step gets it closer.\n"
                + "A hero and a monster may share a space; two monsters cannot.\n"
                + "A new wave of three monsters, one per lane, appears every " + WAVE_INTERVAL + " rounds (same level as highest-level hero).\n"
                + "If you don't kill all the monsters before the next wave, the previous ones will still be present along with the newly spawned monsters.\n"
                + "<------------~ HOW TO WIN ~------------>\n"
                + "Reach the monsters' nexus to win (top row).\n"
                + "if a monster reaches the heroes' nexus (bottom row) you lose! :(");
    }

    // helper to display information about each hero
    private void info() {
        if(session == null) {
            System.out.println("Choose your heroes first.");
            return;
        }
        for(HeroSlot slot : session.getSlots()) {
            Hero hero = slot.getHero();
            // print the hero and whether or not their respawn is pending
            // prints associated information with them as well
            System.out.println(world.getLabel(hero) + " " + hero + (slot.isPendingRespawn() ? " [respawns next round]" : ""));
            System.out.println(hero.getEquipment());
            // lists any status effects on the hero
            for(StatusEffect effect : hero.getEffects())
                System.out.println("  " + effect.getKind() + " " + Math.round(100 * effect.getValue()) + "% | "
                        + (effect.getRemaining() == 0 ? "until removed" : effect.getRemaining() + " round(s) left"));
        }
    }

    /** Helper for printing the combat log lines
     * Colored by type: damage in red, dodges in cyan,
     * everything else is uncolored.
     */
    private void logs(List<String> lines) {
        for(String line : lines) {
            if(line.contains(" damage"))
                System.out.println(colored(line, RED));
            else if(line.endsWith(": dodged"))
                System.out.println(colored(line, CYAN));
            else
                System.out.println(line);
        }
    }

    // helper which returns the hero's inventory items in the respective category
    private List<ItemInstance> items(Hero hero, String category) {
        List<ItemInstance> list = new ArrayList<>();
        for(ItemInstance item : hero.getInventory().getItems())
            if(item.getCategory().equals(category))
                list.add(item);
        return list;
    }

    // Displays board label (M1, M2, M3), its name, level and HP, and where its positon.
    private String describeMonster(Monster monster) {
        return world.getLabel(monster) + " " + monster + " at " + at(world.getPosition(monster));
    }

    // Formats a board position for display.
    // Returns off the board for a unit that has been removed i.e. a fainted hero not on the board anymore
    private String at(Position position) {
        return position == null ? "off the board" : "row " + position.getRow() + ", col " + position.getCol();
    }

    // Helper class
    // Bundles the monster that is waiting to spawn in the corresponding lane
    private static final class QueuedSpawn {
        private final Monster monster;
        private final int lane;

        private QueuedSpawn(Monster monster, int lane) {
            this.monster = monster;
            this.lane = lane;
        }
    }

    /** Prints the statistics summary and saves the report to logs/. Safe to call more than once. */
    private void finishSession() {
        if(statistics == null)
            return;
        System.out.print(statistics.summary());
        try {
            System.out.println("Statistics and action history saved: " + statistics.save(Paths.get("logs"), getSeed()));
        }
        catch(IOException | SecurityException e) {
            System.err.println("Cannot save statistics: " + e.getMessage() + ". Full report follows; copy it to keep a record.");
            System.out.print(statistics.report(getSeed()));
        }
        statistics = null;
    }
}
