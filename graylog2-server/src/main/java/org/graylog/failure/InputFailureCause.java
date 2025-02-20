package org.graylog.failure;

public enum InputFailureCause implements FailureCause{
    INPUT_PARSE("InputParseError"),;

    private final String label;

    InputFailureCause(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
