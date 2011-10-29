package org.graylog2.messagehandlers.gelf;

/**
 * Common base class for all exceptions regarding the GELF message handling
 */
public class GELFException extends Exception {

    public GELFException() {
        super();
    }

    public GELFException(String s) {
        super(s);
    }

    public GELFException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
