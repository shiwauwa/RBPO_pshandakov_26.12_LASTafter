package ru.mtuci.pshandakov.exception;

public class PasswordComplexityException extends AuthException {
    public PasswordComplexityException(String msg) {
        super(msg);
    }
}
