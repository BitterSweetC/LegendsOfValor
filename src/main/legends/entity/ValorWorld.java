package legends.entity;

import java.util.*;

import legends.enums.TileType;

/** Stores the Valor board and the current position of each unit. */
public final class ValorWorld {
    private final TileType[][] tiles;
    private final Map<Hero, Position> heroes = new HashMap<>();
    private final Map<Monster, Position> monsters = new HashMap<>();
    // 保存复活英雄信息
    private final Map<Hero, String> heroLabels = new LinkedHashMap<>();
    private final Map<Monster, String> monsterLabels = new LinkedHashMap<>();//monsters information
    private int nextMonsterNumber = 1;

    public ValorWorld(TileType[][] tiles) {
        this.tiles = new TileType[tiles.length][];
        for(int row = 0; row < tiles.length; row++)
            this.tiles[row] = tiles[row].clone();
    }

    public TileType getTile(int row, int col) {
        return tiles[row][col];
    }

    public boolean placeHero(Hero hero, int row, int col) {
        if(heroes.containsKey(hero) || !ableToEnter(heroes, row, col))
            return false;

        setHeroPosition(hero, new Position(row, col));
        heroLabels.putIfAbsent(hero, "H" + (heroLabels.size() + 1));//放格子
        return true;
    }

    public boolean placeMonster(Monster monster, int row, int col) {
        if(monsters.containsKey(monster) || !ableToEnter(monsters, row, col))
            return false;

        monsters.put(monster, new Position(row, col));
        //没怪物放
        if(!monsterLabels.containsKey(monster))
            monsterLabels.put(monster, "M" + nextMonsterNumber++);
        return true;
    }

    public void removeHero(Hero hero) {
        setHeroPosition(hero, null);
    }

    public void removeMonster(Monster monster) {
        monsters.remove(monster);
    }

    //得到格子信息
    public TileType getTile(Position position) {
        return getTile(position.getRow(), position.getCol());
    }

    public boolean placeHero(Hero hero, Position position) {
        return placeHero(hero, position.getRow(), position.getCol());
    }

    public boolean placeMonster(Monster monster, Position position) {
        return placeMonster(monster, position.getRow(), position.getCol());
    }

    //位置
    public Position getPosition(Combatant unit) {
        return positionOf(unit);
    }

    public int getRow(Combatant unit) {
        Position position = positionOf(unit);
        return position == null ? -1 : position.getRow();
    }

    public int getCol(Combatant unit) {
        Position position = positionOf(unit);
        return position == null ? -1 : position.getCol();
    }

    public boolean moveHero(Hero hero, int rowChange, int colChange) {
        Position current = heroes.get(hero);
        if (current == null || !hero.isAlive()) {
            return false;
        }

        int row = current.getRow();
        int col = current.getCol();

        if (colChange == 0) {
            if (rowChange == -1) {
                row--;
            } else if (rowChange == 1) {
                row++;
            } else {
                return false;
            }
        } else if (rowChange == 0) {
            if (colChange == -1) {
                col--;
            } else if (colChange == 1) {
                col++;
            } else {
                return false;
            }
        } else {
            return false;
        }

        if (!ableToEnter(heroes, row, col) || isBlockedByMonster(current, row, col)) {
            return false;
        }

        setHeroPosition(hero, new Position(row, col));
        return true;
    }

    //阻挡过不去
    public boolean isBlockedByEnemy(Hero hero, int row, int col) {
        Position current = heroes.get(hero);
        return current != null && isBlockedByMonster(current, row, col);
    }

    public boolean isBlockedByEnemy(Monster monster, int row, int col) {
        Position current = monsters.get(monster);
        return current != null && isBlockedByHero(current, row, col);
    }

    public Position getHeroSpawn(int lane) {
        if(lane < 0 || lane >= 3)
            return null;

        return new Position(tiles.length - 1, lane * 3);
    }

    public Position getMonsterSpawn(int lane) {
        if(lane < 0 || lane >= 3)
            return null;

        return new Position(0, lane * 3 + 1);
    }

    public int getColCount() {
        return tiles.length == 0 ? 0 : tiles[0].length;
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < tiles.length && col >= 0 && col < tiles[row].length;
    }

    //返回lane
    public int getLane(int col) {
        if(col < 0 || col >= getColCount() || col % 3 == 2)
            return -1;
        return col / 3;
    }

    public Hero getHeroAt(int row, int col) {
        for(Map.Entry<Hero, Position> entry : heroes.entrySet())
            if(entry.getValue().getRow() == row && entry.getValue().getCol() == col)
                return entry.getKey();
        return null;
    }

    public Monster getMonsterAt(int row, int col) {
        for(Map.Entry<Monster, Position> entry : monsters.entrySet())
            if(entry.getValue().getRow() == row && entry.getValue().getCol() == col)
                return entry.getKey();
        return null;
    }

    public boolean hasHeroAt(int row, int col) {
        return getHeroAt(row, col) != null;
    }

    public boolean hasMonsterAt(int row, int col) {
        return getMonsterAt(row, col) != null;
    }

    //get the heroes
    public List<Hero> getHeroes() {
        List<Hero> result = new ArrayList<>();
        for(Hero hero : heroLabels.keySet())
            if(heroes.containsKey(hero))
                result.add(hero);
        return result;
    }

    //get monsters
    public List<Monster> getMonsters() {
        List<Monster> result = new ArrayList<>();
        for(Monster monster : monsterLabels.keySet())
            if(monsters.containsKey(monster))
                result.add(monster);
        return result;
    }

    //返回label
    public String getLabel(Combatant unit) {
        return unit instanceof Hero ? heroLabels.get(unit) : monsterLabels.get(unit);
    }

    //到没到基地
    public boolean hasHeroReachedEnemyNexus() {
        for(Position position : heroes.values())
            if(isMonsterNexus(position.getRow(), position.getCol()))
                return true;
        return false;
    }

    public boolean hasMonsterReachedHeroNexus() {
        for(Position position : monsters.values())
            if(isHeroNexus(position.getRow(), position.getCol()))
                return true;
        return false;
    }

    //生成地图
    public String render(boolean colorEnabled) {
        StringBuilder out = new StringBuilder();

        out.append("   ");
        for(int col = 0; col < getColCount(); col++)
            out.append(String.format("    %d      ", col));
        out.append("\n");

        for(int row = 0; row < tiles.length; row++) {
            StringBuilder top = new StringBuilder("   ");
            StringBuilder middle = new StringBuilder(String.format("%2d ", row));
            StringBuilder bottom = new StringBuilder("   ");
            for(int col = 0; col < tiles[row].length; col++) {
                char symbol = tileSymbol(tiles[row][col]);
                String edge = symbol + " - " + symbol + " - " + symbol;

                top.append(edge).append("  ");
                bottom.append(edge).append("  ");
                middle.append('|').append(cellContents(row, col)).append("|  ");
            }
            out.append(top).append('\n').append(middle).append('\n').append(bottom).append("\n\n");
        }

        out.append("Legend: ").append(legendEntry(TileType.NEXUS, "Nexus"))
                .append(legendEntry(TileType.BLOCKED, "Impassible"))
                .append(legendEntry(TileType.OBSTACLE, "Obstacle"))
                .append(legendEntry(TileType.PLAIN, "Plain"))
                .append(legendEntry(TileType.BUSH, "Bush"))
                .append(legendEntry(TileType.CAVE, "Cave"))
                .append(legendEntry(TileType.KOULOU, "Koulou")).append('\n');
        out.append("        Top row = monsters' Nexus, bottom row = heroes' Nexus. H1=hero, M1=monster.\n");

        for(Hero hero : getHeroes())
            out.append(String.format("  %-3s  %-20s %s  HP %d/%d  MP %d/%d%n",
                    heroLabels.get(hero), hero.getName(), heroes.get(hero),
                    hero.getHp(), hero.getMaxHp(), hero.getMp(), hero.getMaxMp()));
        for(Monster monster : getMonsters())
            out.append(String.format("  %-3s  %-20s %s  HP %d/%d  Lv %d%n",
                    monsterLabels.get(monster), monster.getName(), monsters.get(monster),
                    monster.getHp(), monster.getMaxHp(), monster.getLevel()));

        return out.toString();
    }

    //格子里面内容
    private String cellContents(int row, int col) {
        if(tiles[row][col] == TileType.BLOCKED)
            return " X X X ";

        Hero hero = getHeroAt(row, col);
        Monster monster = getMonsterAt(row, col);
        String heroText = String.format("%-2s", hero == null ? "" : heroLabels.get(hero));
        String monsterText = String.format("%-3s", monster == null ? "" : monsterLabels.get(monster));
        return " " + heroText + " " + monsterText;
    }

    private static String legendEntry(TileType type, String name) {
        return tileSymbol(type) + "=" + name + "  ";
    }

    private static char tileSymbol(TileType type) {
        switch(type) {
            case NEXUS: return 'N';
            case BLOCKED: return 'I';
            case OBSTACLE: return 'O';
            case PLAIN: return 'P';
            case BUSH: return 'B';
            case CAVE: return 'C';
            case KOULOU: return 'K';
            default: return '?';
        }
    }

    public boolean isHeroNexus(int row, int col) {
        if (row < 0 || row != tiles.length - 1)
            return false;
        return col >= 0 && col < tiles[row].length && tiles[row][col] == TileType.NEXUS;
    }

    public boolean isMonsterNexus(int row, int col) {
        if(row != 0 || tiles.length == 0)
            return false;
        return col >= 0 && col < tiles[row].length && tiles[row][col] == TileType.NEXUS;
    }

    public List<Monster> getAttackableMonsters(Hero hero) {
        Position heroPosition = heroes.get(hero);
        if(heroPosition == null || !hero.isAlive())
            return Collections.emptyList();

        List<Monster> targets = new ArrayList<>();

        for(Map.Entry<Monster, Position> entry : monsters.entrySet()){
            Monster monster = entry.getKey();
            Position monsterPosition = entry.getValue();

            int rowDistance = Math.abs(heroPosition.getRow() - monsterPosition.getRow());
            int colDistance = Math.abs(heroPosition.getCol() - monsterPosition.getCol());

            if (monster.isAlive() && rowDistance <= 1 && colDistance <= 1)
                targets.add(monster);
        }

        return targets;
    }

    public List<Hero> getAttackableHeroes(Monster monster) {
        Position monsterPosition = monsters.get(monster);
        if(monsterPosition == null || !monster.isAlive())
            return Collections.emptyList();

        List<Hero> targets = new ArrayList<>();

        for(Map.Entry<Hero, Position> entry : heroes.entrySet()){
            Hero hero = entry.getKey();
            Position heroPosition = entry.getValue();

            int rowDistance = Math.abs(heroPosition.getRow() - monsterPosition.getRow());
            int colDistance = Math.abs(heroPosition.getCol() - monsterPosition.getCol());

            if (hero.isAlive() && rowDistance <= 1 && colDistance <= 1)
                targets.add(hero);
        }

        targets.sort(Comparator.comparingLong(Hero::getHp));   // weakest hero first
        return targets;
    }

    //
    public boolean advanceMonster(Monster monster) {
        Position current = monsters.get(monster);
        if(current == null || !monster.isAlive())
            return false;

        Position step = nextStepTowardNexus(current);
        if(step == null)
            return false;
        monsters.put(monster, step);
        return true;
    }

    /**
     * Breadth-first search inside the monster's lane for the shortest route to the heroes' Nexus row,
     * moving only forward or sideways around walls and obstacles. Returns the first step of that route.
     * If the lane is cut off, it heads for the furthest reachable row instead; returns null when no legal
     * step gains ground. The first step obeys every movement rule (no friendly
     * monster in the space, never past a hero); later steps consider terrain and heroes, so the monster
     * does not wander sideways when a hero blocks its lane.
     */
    private Position nextStepTowardNexus(Position start) {
        if(start.getRow() == tiles.length - 1)
            return null;   // already in the heroes' Nexus row

        int lane = getLane(start.getCol());
        Map<Position, Position> firstStep = new HashMap<>();
        ArrayDeque<Position> queue = new ArrayDeque<>();
        int[][] moves = { { 1, 0 }, { 0, -1 }, { 0, 1 } };   // forward first, so it wins ties

        for(int[] m : moves) {
            Position next = start.offset(m[0], m[1]);
            if(getLane(next.getCol()) == lane && canMonsterEnter(start, next.getRow(), next.getCol())) {
                firstStep.put(next, next);
                queue.add(next);
            }
        }

        Position furthest = null;   // used when a hero or obstacle cuts the lane off completely
        while(!queue.isEmpty()) {
            Position p = queue.poll();
            if(p.getRow() == tiles.length - 1)
                return firstStep.get(p);
            if(furthest == null || p.getRow() > furthest.getRow())
                furthest = p;
            for(int[] m : moves) {
                Position next = p.offset(m[0], m[1]);
                if(!firstStep.containsKey(next) && !next.equals(start) && isInBounds(next.getRow(), next.getCol())
                        && getLane(next.getCol()) == lane
                        && tiles[next.getRow()][next.getCol()] != TileType.OBSTACLE
                        && tiles[next.getRow()][next.getCol()] != TileType.BLOCKED
                        && !isBlockedByHero(start, next.getRow(), next.getCol())) {
                    firstStep.put(next, firstStep.get(p));
                    queue.add(next);
                }
            }
        }
        // No full route: still close in as far as possible, but never step sideways without gaining ground.
        return furthest != null && furthest.getRow() > start.getRow() ? firstStep.get(furthest) : null;
    }

    private boolean canMonsterEnter(Position current, int row, int col) {
        return ableToEnter(monsters, row, col) && !isBlockedByHero(current, row, col);
    }

    private boolean isBlockedByMonster(Position hero, int row, int col) {
        int lane = getLane(col);
        for(Map.Entry<Monster, Position> entry : monsters.entrySet()) {
            Position monster = entry.getValue();
            if(entry.getKey().isAlive() && getLane(monster.getCol()) == lane && monster.getRow() <= hero.getRow() && row < monster.getRow())
                return true;
        }
        return false;
    }

    // heroes if can enter
    private boolean isBlockedByHero(Position monster, int row, int col) {
        int lane = getLane(col);
        for(Map.Entry<Hero, Position> entry : heroes.entrySet()) {
            Position hero = entry.getValue();
            if(entry.getKey().isAlive() && getLane(hero.getCol()) == lane
                    && hero.getRow() >= monster.getRow() && row > hero.getRow())
                return true;
        }
        return false;
    }

    //the heroes can teleport around the existing heroes
    public List<Position> getTeleportTargets(Hero hero, Hero teammate) {
        Position from = heroes.get(hero);
        Position mate = heroes.get(teammate);
        if(from == null || mate == null || hero == teammate || !hero.isAlive() || !teammate.isAlive() || getLane(from.getCol()) == getLane(mate.getCol()))
            return Collections.emptyList();

        List<Position> targets = new ArrayList<>();
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 } };   // left, right, behind
        for(int[] offset : offsets) {
            int row = mate.getRow() + offset[0];
            int col = mate.getCol() + offset[1];
            if(ableToEnter(heroes, row, col) && !isPastMonster(row, col))
                targets.add(new Position(row, col));
        }
        return targets;
    }

    //teleport hero to the position
    public boolean teleportHero(Hero hero, Hero teammate, Position target) {
        if(target == null || !getTeleportTargets(hero, teammate).contains(target))
            return false;
        setHeroPosition(hero, target);
        return true;
    }

    public boolean recallHero(Hero hero, Position home) {
        Position current = heroes.get(hero);
        if(current == null || home == null || current.equals(home) || !hero.isAlive()
                || !isHeroNexus(home.getRow(), home.getCol()) || !ableToEnter(heroes, home.getRow(), home.getCol()))
            return false;
        setHeroPosition(hero, home);
        return true;
    }

    public boolean respawnHero(Hero hero, Position home) {
        if(home == null || heroes.containsKey(hero) || !isHeroNexus(home.getRow(), home.getCol()))
            return false;
        return placeHero(hero, home);
    }

    //find the obstacles you can remove
    public List<Position> getRemovableObstacles(Hero hero) {
        Position from = heroes.get(hero);
        if(from == null || !hero.isAlive())
            return Collections.emptyList();

        List<Position> targets = new ArrayList<>();
        int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for(int[] offset : offsets) {
            int row = from.getRow() + offset[0];
            int col = from.getCol() + offset[1];
            if(isInBounds(row, col) && tiles[row][col] == TileType.OBSTACLE)
                targets.add(new Position(row, col));
        }
        return targets;
    }

    //heroes are able to remove obstacles on the way
    public boolean removeObstacle(Hero hero, Position target) {
        if(target == null || !getRemovableObstacles(hero).contains(target))
            return false;
        tiles[target.getRow()][target.getCol()] = TileType.PLAIN;
        return true;
    }

    private void setHeroPosition(Hero hero, Position position) {
        hero.clearBonus();                 //remove the old space's bonus first
        if(position == null) {
            heroes.remove(hero);                  // off the map: no bonus
            return;
        }
        heroes.put(hero, position);
        String stat = bonusStatus(tiles[position.getRow()][position.getCol()]);
        if(stat != null)
            hero.setTerrainState(stat, TERRAIN_BONUS);
    }

    //whether the monster is alive
    private boolean isPastMonster(int row, int col) {
        int lane = getLane(col);
        for(Map.Entry<Monster, Position> entry : monsters.entrySet())
            if(entry.getKey().isAlive() && getLane(entry.getValue().getCol()) == lane
                    && row < entry.getValue().getRow())
                return true;
        return false;
    }

    //get the type of unit
    private Position positionOf(Combatant unit) {
        return unit instanceof Hero ? heroes.get(unit) : monsters.get(unit);
    }

    //if you can enter the location
    private boolean ableToEnter(Map<? extends Combatant, Position> allies, int row, int col) {
        if(row < 0 || row >= tiles.length || col < 0 || col >= tiles[row].length)
            return false;

        if(tiles[row][col] == TileType.BLOCKED || tiles[row][col] == TileType.OBSTACLE)
            return false;

        for(Position position : allies.values())
            if(position.getRow() == row && position.getCol() == col)
                return false;

        return true;
    }

    private static final double TERRAIN_BONUS = 0.10;

    private static String bonusStatus(TileType type) {
        switch(type) {
            case BUSH:   return "DEX";
            case CAVE:   return "AGI";
            case KOULOU: return "STR";
            default:     return null;
        }
    }
}
