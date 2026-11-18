package legends.entity;

public final class Position {
    private final int row, col;

    public Position(int row, int col) {
        // todo: bounds check based on map
        this.row = row;
        this.col = col;
    }

    // getters
    public int getRow() { return row; }
    public int getCol() { return col; }

    // todo: setters if needed
}