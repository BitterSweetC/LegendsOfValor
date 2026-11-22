package legends.enums;

/**
 * How often a new wave of monsters spawn
 * Based on the difficulty of the game the user picks
 */

public enum LvDifficulty {
    EASY("Easy", 8),
    MEDIUM("Medium", 6),
    HARD("Hard", 3);

    private final String label;
    private final int waveInterval;

    LvDifficulty(String label, int waveInterval) {
        this.label = label;
        this.waveInterval = waveInterval;
    }

    public int getWaveInterval() {
        return waveInterval;
    }

    @Override
    public String toString() {
        return label + " (new monsters every " + waveInterval + " rounds)";
    }
}
