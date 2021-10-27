package org.graylog.plugins.views.search.rest;

public class ValidationExplanationDTO {
    private final String index;
    private final boolean valid;
    private final String explanation;
    private final String error;

    public ValidationExplanationDTO(String index, boolean valid, String explanation, String error) {
        this.index = index;
        this.valid = valid;
        this.explanation = explanation;
        this.error = error;
    }

    public String getIndex() {
        return index;
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
