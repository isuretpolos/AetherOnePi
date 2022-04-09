package de.isuret.polos.AetherOnePi.utils.cards;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CardMakerTest {

    @Test
    public void testCardMaker() throws Exception {

        List<RadionicLine> lines = new ArrayList<>();
        lines.add(new RadionicLine(500, 0, "Arnica", Color.GREEN));
        lines.add(new RadionicLine(500, 45, "Belladonna", Color.RED));
        lines.add(new RadionicLine(500, 90, "grounding", Color.BLACK));
        lines.add(new RadionicLine(500, 180, "Sulphur", Color.YELLOW));

        CardMaker cardMaker = new CardMaker();
        cardMaker.make(lines);

        File file = new File("target/card.png");
        cardMaker.save(file);
    }

}
