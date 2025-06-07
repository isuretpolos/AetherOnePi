package de.isuret.polos.AetherOnePi.enums;

public enum HighFiveAlphabet {

    __,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z;

    public static String getByPosition(int pos) {

        int i=0;

        for (HighFiveAlphabet abc : values()) {

            if (i == pos) {
                return abc.name();
            }

            i++;
        }

        return __.name();
    }

    public static Integer getIntegerValue(HighFiveAlphabet a) {

        int i=0;

        for (HighFiveAlphabet abc : values()) {
            if (abc == a) {
                return i;
            }

            i++;
        }

        return null;
    }

    public static int count() {
        return values().length;
    }
}
