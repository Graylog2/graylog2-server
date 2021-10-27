package org.graylog.plugins.views.search.engine;

public class ValidationExplanation {
    private final String index;
    private int shard = -1;
    private final boolean valid;
    private final String explanation;
    private final String error;

    public ValidationExplanation(String index, int shard, boolean valid, String explanation, String error) {
        this.index = index;
        this.shard = shard;
        this.valid = valid;
        this.explanation = explanation;
        this.error = error;
    }

    public String getIndex() {
        return index;
    }

    public int getShard() {
        return shard;
    }

    public boolean isValid() {
        return valid;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getError() {
        return error;
    }
}
