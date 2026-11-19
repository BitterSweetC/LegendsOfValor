package legends.entity.hero;

import java.util.Arrays;
import java.util.List;

import legends.config.GameConfig;
import legends.entity.Hero;
import legends.entity.template.HeroTto;

public class Warrior extends Hero {
    private static final List<String> FAVORED = Arrays.asList("STR", "AGI");

    public Warrior(HeroTto definition, GameConfig config) {
        super(definition, config);
    }

    @Override
    public String getHeroClass() {
        return "WARRIOR";
    }

    @Override
    protected List<String> getFavoredAttributes() {
        return FAVORED;
    }
}
