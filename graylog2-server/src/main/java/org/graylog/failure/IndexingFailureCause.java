package org.graylog.failure;

public enum IndexingFailureCause implements FailureCause {
    MappingError("MappingError"),
    UNKNOWN("UNKNOWN");

    private final String label;

    IndexingFailureCause(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
