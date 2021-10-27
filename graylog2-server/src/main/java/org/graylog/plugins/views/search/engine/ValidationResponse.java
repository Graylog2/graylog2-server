package org.graylog.plugins.views.search.engine;

import java.util.List;

public class ValidationResponse {

    private boolean valid;
    private List<ValidationExplanation> explanations;

    public ValidationResponse(boolean valid, List<ValidationExplanation> explanations) {
        this.valid = valid;
        this.explanations = explanations;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationExplanation> getExplanations() {
        return explanations;
    }
}
