package de.isuret.polos;

import org.junit.Test;

public class BroadCastServiceTest {

    @Test
    public void test() {

        String signature = "Ficus Papa Augen";

        for (int x = 0; x < signature.length(); x++) {
            char c = signature.charAt(x);
            int i = c - '0';
            if (i < 0) i = i * -1;
            String stringPart = String.valueOf(i);

            for (int y = 0; y < stringPart.length(); y++) {

                char part = stringPart.charAt(y);

                try {
                    Integer n = Integer.parseInt(Character.toString(part));
                    System.out.println(n);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testReplacement() {
        String test = "bla bla (inside) outside";
        System.out.println(test);
        String replaced = test.replaceAll("\\(","").replaceAll("\\)","");
        System.out.println(replaced);
    }
}
