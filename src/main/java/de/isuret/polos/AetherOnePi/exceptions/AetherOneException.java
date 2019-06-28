package de.isuret.polos.AetherOnePi.exceptions;

/**
 * Every exception thrown by AetherOne
 */
public class AetherOneException extends Exception {

    public AetherOneException(String message) {
        super(message);
    }

    public AetherOneException(String message, Throwable e) {
        super(message,e);
    }
}
