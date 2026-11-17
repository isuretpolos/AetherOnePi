package de.isuret.polos.aopi.session;

import java.io.IOException;

/** Reimplements the original question sequence using cached acquired values, without a software fallback. */
public class EssentialQuestions {

    @FunctionalInterface
    public interface Draws {
        int nextInt(int bound) throws IOException;
    }

    private final Draws draws;

    public EssentialQuestions(Draws draws) {
        this.draws = draws;
    }

    public String ask() throws IOException {
        StringBuilder text = new StringBuilder("ESSENTIAL QUESTIONS\n\nRandomized reflection results; these do not establish actual consent.\n\n");
        text.append("Appropriateness (0-100): ").append(highest(101)).append("%\n\n");
        boolean permission = answer(text, "Do I have permission to work with this person?");
        if (permission) {
            answer(text, "Is it in the person's best and highest good that I work with them?");
            answer(text, "Is it in my best and highest good?");
        }
        return text.toString();
    }

    private boolean answer(StringBuilder text, String question) throws IOException {
        int value = vitality();
        text.append(question).append('\n').append(value > 500 ? "YES" : "NO").append(" (GV ")
                .append(value).append(")\n\n");
        if (value <= 500) {
            int later = vitality();
            text.append("Will I ever have permission? ").append(later > 500 ? "YES" : "NO")
                    .append(" (GV ").append(later).append(")\n");
            if (later > 500) {
                String best = "";
                int maximum = -1;
                for (String when : new String[]{"In one week", "In one month", "In two months", "In three months",
                        "In six months", "In one year", "In two years"}) {
                    int score = vitality();
                    if (score > maximum) {
                        maximum = score;
                        best = when;
                    }
                }
                text.append(best).append('\n');
            }
            text.append('\n');
        }
        return value > 500;
    }

    private int highest(int bound) throws IOException {
        return Math.max(draws.nextInt(bound), Math.max(draws.nextInt(bound), draws.nextInt(bound)));
    }

    private int vitality() throws IOException {
        int value = highest(1001);
        if (value > 950) {
            for (int attempt = 0; attempt < 256; attempt++) {
                int dice = draws.nextInt(101);
                if (dice < 50) {
                    return value;
                }
                value += dice;
            }
            throw new IOException("Vitality extension did not finish; check the acquired hotbits source.");
        }
        return value;
    }
}
