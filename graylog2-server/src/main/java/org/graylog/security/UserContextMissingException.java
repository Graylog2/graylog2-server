package org.graylog.security;

public class UserContextMissingException extends Exception {

    public UserContextMissingException(String s) {
        super(s);
    }
}
