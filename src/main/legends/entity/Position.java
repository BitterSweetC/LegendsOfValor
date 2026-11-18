package legends.entity;

/** An immutable row/column coordinate on a game board. */
public final class Position {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /** Returns the position shifted by the given row and column offsets. */
    public Position offset(int rowChange, int colChange) {
        return new Position(row + rowChange, col + colChange);
    }

    @Override
    public boolean equals(Object other) {
        if(this == other)
            return true;
        if(!(other instanceof Position))
            return false;
        Position that = (Position) other;
        return row == that.row && col == that.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
