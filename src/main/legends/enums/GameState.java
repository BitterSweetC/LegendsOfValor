package legends.enums;

// Flow of Game State in Legends of Valor: SETUP -> HERO_PHASE -> MONSTER_PHASE -> HERO_PHASE -> ... -> GAME_OVER
// Flow of Game State in Legends Monsters and Heroes: SETUP -> WORLD <-> MARKET, WORLD -> BATTLE -> WORLD (or GAME_OVER)

public enum GameState {
    // Shared by both games
    SETUP, // Start of a game: choose heroes (and lanes in Valor), then build the world and session.
    GAME_OVER, // The game has ended (all heroes fainted, or a Nexus was reached in Valor). Offers restart or exit.
    EXIT, // Leave the current game and return to the game selector.

    // Monsters and Heroes only
    WORLD, // The party is exploring the map
    BATTLE, // A random encounter occurred and heroes and monsters are battling
    MARKET, // The party is standing on a market tile

    // Legends of Valor only

    /** The player picks one action for each living hero (move, attack, spell, potion, equip,
     *  teleport, recall, clear obstacle or pass). Buying and selling at the Nexus are free
     *  and do not end this phase. Ends when every living hero has acted or a Nexus is reached. */
    HERO_PHASE,

    /** Each monster either attacks a hero in range or moves one space forward. Then the round
     *  ends: heroes recover, fallen heroes respawn, and a new wave spawns when one is due. */
    MONSTER_PHASE
}