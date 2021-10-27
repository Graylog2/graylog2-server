package org.graylog.plugins.views.search.rest;

import java.util.List;

public class ValidationResponseDTO {
    private final boolean valid;
    private final List<ValidationExplanationDTO> explanations;

    public ValidationResponseDTO(boolean valid, List<ValidationExplanationDTO> explanations) {
        this.valid = valid;
        this.explanations = explanations;
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationExplanationDTO> getExplanations() {
        return explanations;
    }
}
