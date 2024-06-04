package org.graylog2.categories.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BulkCategoryResponse(@JsonProperty("errors") List<String> errors) {
    public BulkCategoryResponse(List<String> errors) {
        this.errors = errors;
    }
}
