package de.isuret.polos.AetherOnePi.exceptions;

/**
 * Connection to the True Random Source failed or not available
 */
public class HotbitsCollectingException extends Exception {

    public HotbitsCollectingException(String message) {
        super(message);
    }

    public HotbitsCollectingException(String message, Throwable e) {
        super(message,e);
    }
}
