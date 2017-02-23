package org.graylog.plugins.cef.parser;

public class ParserException extends Exception {

    public ParserException(String msg) {
        super(msg);
    }

    public ParserException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

}