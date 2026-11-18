package legends.entity;

import java.util.*;

import legends.enums.GameState;

/** Valor game state: world, selected heroes, phase, round, action tracking, spawns, respawns, winner. */
public final class LvGameSession {
    public static final int LANES = 3; // hero lanes

    private final LvWorld world; // assuming LvWorld exists for now need to verify when partner B is done
    private final List<HeroSlot> slots = new ArrayList<>(); // Exactly three heroes total, one per lane
    private int round = 1;
    private GameState state = GameState.HERO_PHASE; // default to heroes acting first
    private boolean heroesWon; // only meaningful once finish() is called and state == GAME_OVER

    /** Assuming that heroes.get(i) starts in lanes[i] */
    // todo: tweak lane logic if necessary ?
    public LvGameSession(LvWorld world, List<Hero> heroes, int[] lanes) {
        if (world == null || heroes == null || lanes == null)
            throw new IllegalArgumentException("World, heroes and lanes are required");
        if (heroes.size() != LANES || lanes.length != LANES)
            throw new IllegalArgumentException("Exactly three heroes are required");

        boolean[] used = new boolean[LANES];
        for (int lane : lanes) {
            if (lane < 0 || lane >= LANES || used[lane])
                throw new IllegalArgumentException("Each hero needs a different lane (0-2)");
            used[lane] = true;
        }

        // Verify the same hero object is not being added twice
        Set<Hero> distinct = Collections.newSetFromMap(new IdentityHashMap<Hero, Boolean>());
        for (Hero hero : heroes)
            if (hero == null || !distinct.add(hero))
                throw new IllegalArgumentException("Each lane needs a different hero");

        this.world = world;
        // using H1, H2, H3 as hero labels
        for (int i = 0; i < LANES; i++) // create the 3 valor heroes in their respective lanes and spawns
            slots.add(new HeroSlot("H" + (i + 1), heroes.get(i), lanes[i], world.getHeroSpawn(lanes[i])));
    }

    public LvWorld getWorld() { return world; }

    public List<HeroSlot> getSlots() { return Collections.unmodifiableList(slots); }

    public HeroSlot slotOf(Hero hero) {
        for (HeroSlot slot : slots)
            if (slot.getHero() == hero)
                return slot;
        throw new IllegalArgumentException("Not a hero in this game");
    }

    public String heroLabel(Hero hero) { return slotOf(hero).getLabel(); }

    public boolean isAlive(Hero hero) { return hero.getHp() > 0; }

    public List<Hero> getLivingHeroes() {
        List<Hero> living = new ArrayList<>();
        for (HeroSlot slot : slots)
            if (isAlive(slot.getHero()))
                living.add(slot.getHero());
        return living;
    }

    public int getHighestHeroLevel() {
        int max = 1;
        for (HeroSlot slot : slots)
            max = Math.max(max, slot.getHero().getLevel());
        return max;
    }

    // phase of the game we're currently in
    public GameState getState() { return state; }

    // starts the hero phase, reset every heroes' turn
    // if the game is over then the hero phase is ignored
    public void beginHeroPhase() {
        if (isOver())
            return;
        state = GameState.HERO_PHASE;
        for (HeroSlot slot : slots)
            slot.setActed(false);
    }

    // starts the monster phase
    // if the game is over then the monster phase is ignored
    public void beginMonsterPhase() {
        if (!isOver())
            state = GameState.MONSTER_PHASE;
    }

    // next living hero who has not acted this round
    // null when the hero phase is over
    public HeroSlot nextToAct() {
        for (HeroSlot slot : slots)
            if (!slot.hasActed() && isAlive(slot.getHero()))
                return slot;
        return null;
    }

    // call only after a valid action
    // invalid commands or actions that don't use a turn shouldn't call this
    public void markActed(Hero hero) { slotOf(hero).setActed(true); }

    public int getRound() { return round; }
    public void endRound() { round++; }

    // Call when a hero faints
    // Set it's respawn to pending until the round is over
    public void scheduleRespawn(Hero hero) { slotOf(hero).setPendingRespawn(true); }

    // List of heroes waiting to respawn
    public List<HeroSlot> getPendingRespawns() {
        List<HeroSlot> pending = new ArrayList<>();
        for (HeroSlot slot : slots)
            if (slot.isPendingRespawn())
                pending.add(slot);
        return pending;
    }

    // Call only after the world has actually placed the hero
    // if placement failed, keep it pending.
    public void completeRespawn(Hero hero) { slotOf(hero).setPendingRespawn(false); }

    // end of the game
    public boolean isOver() { return state == GameState.GAME_OVER; }

    public boolean didHeroesWin() {
        if (!isOver())
            throw new IllegalStateException("Game is not over yet");
        return heroesWon;
    }

    public void finish(boolean heroesWon) {
        if (isOver())
            return;
        this.heroesWon = heroesWon;
        state = GameState.GAME_OVER;
    }
}