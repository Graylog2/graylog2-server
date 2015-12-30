package org.graylog.plugins.messageprocessor;

public class FieldSet {
    private static final FieldSet INSTANCE = new FieldSet();

    public static FieldSet empty() {
        return INSTANCE;
    }
}
