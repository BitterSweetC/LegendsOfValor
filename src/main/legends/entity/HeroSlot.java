package legends.entity;

/** Hero Wrapper Class
 * Used by Legends of Valor
 * Pairs one Hero with the Valor-only information about that hero
 * Work will be delegated to the inner wrapped hero object
 * This class is just useful to access the Valor-only state
 * */

public final class HeroSlot {
    private final String label;        // hero label on the board
    private final Hero hero;           // hero object we will delegate work to
    private final int lane;            // heroes' designated lane either 0, 1, or 2
    private final Position home;       // heroes' spawn cell
    private boolean acted;             // flag to determine if hero has used up their turn
    private boolean pendingRespawn;    // flag to determine if the hero died and waiting to come respawn next round

    public HeroSlot(String label, Hero hero, int lane, Position home) {
        // Validation checks
        if (label == null || hero == null || home == null)
            throw new IllegalArgumentException("Label, hero and home are required");
        if (lane < 0 || lane > 2)
            throw new IllegalArgumentException("Lane must be 0-2");

        this.label = label;
        this.hero = hero;
        this.lane = lane;
        this.home = home;

        this.acted = false;
        this.pendingRespawn = false;
    }

    // getters
    public String getLabel() { return label; }
    public Hero getHero() { return hero; }
    public int getLane() { return lane; }
    public Position getHome() { return home; }
    public boolean hasActed() { return acted; }
    public boolean isPendingRespawn() { return pendingRespawn; }
    public void setPendingRespawn(boolean pendingRespawn) { this.pendingRespawn = pendingRespawn; }

    // setters
    public void setActed(boolean acted) { this.acted = acted; }
}