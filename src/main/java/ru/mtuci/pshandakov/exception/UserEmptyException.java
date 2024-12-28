package ru.mtuci.pshandakov.exception;

public class UserEmptyException extends Exception {

    public UserEmptyException() {
        super("User is empty");
    }
}
