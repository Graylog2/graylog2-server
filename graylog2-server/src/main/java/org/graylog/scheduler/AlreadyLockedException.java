package org.graylog.scheduler;

public class AlreadyLockedException extends Exception {
    public AlreadyLockedException(String message) {
        super(message);
    }
}
