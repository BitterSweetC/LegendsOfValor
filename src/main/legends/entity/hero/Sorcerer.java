package legends.entity.hero;

import java.util.Arrays;
import java.util.List;

import legends.config.GameConfig;
import legends.entity.Hero;
import legends.entity.template.HeroTto;

public class Sorcerer extends Hero {
    private static final List<String> FAVORED = Arrays.asList("DEX", "AGI");

    public Sorcerer(HeroTto definition, GameConfig config) {
        super(definition, config);
    }

    @Override
    public String getHeroClass() {
        return "SORCERER";
    }

    @Override
    protected List<String> getFavoredAttributes() {
        return FAVORED;
    }
}
