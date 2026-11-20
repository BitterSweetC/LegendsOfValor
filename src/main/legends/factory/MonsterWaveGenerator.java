package legends.factory;

import java.util.*;

import legends.config.GameConfig;
import legends.entity.Monster;
import legends.utils.RandomWeights;

// Creates one wave of equally leveled monsters, one per lane
public final class MonsterWaveGenerator {
    public List<Monster> generate(GameConfig config, Random random, int level, int count) {
        if(config.getMonsterTtos().isEmpty())
            throw new IllegalStateException("No monster definitions are loaded");

        int[] weights = new int[config.getMonsterTtos().size()];
        for(int i = 0; i < weights.length; i++)
            weights[i] = config.getMonsterTtos().get(i).getWeight();

        List<Monster> wave = new ArrayList<>();
        for(int i = 0; i < count; i++)
            wave.add(MonsterFactory.create(
                    config.getMonsterTtos().get(RandomWeights.pick(random, weights)), Math.max(1, level)));
        return wave;
    }
}