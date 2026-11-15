package legends.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import legends.entity.ValorWorld;
import legends.enums.TileType;

public class VWorldGenerator {
    public ValorWorld generate(Random random) {
        TileType[][] tiles = new TileType[8][8];
        List<TileType> terrain = new ArrayList<>();

        // 准备路线内部的 36 个地形
        for (int i = 0; i < 36; i++) {
            if (i < 4) {
                terrain.add(TileType.OBSTACLE);
            } else if (i < 12) {
                terrain.add(TileType.BUSH);
            } else if (i < 20) {
                terrain.add(TileType.CAVE);
            } else if (i < 28) {
                terrain.add(TileType.KOULOU);
            } else {
                terrain.add(TileType.PLAIN);
            }
        }

        Collections.shuffle(terrain, random);
        int index = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (col == 2 || col == 5) {
                    tiles[row][col] = TileType.BLOCKED;
                } else if (row == 0 || row == 7) {
                    tiles[row][col] = TileType.NEXUS;
                } else {
                    tiles[row][col] = terrain.get(index);
                    index++;
                }
            }
        }

        return new ValorWorld(tiles);
    }
}
