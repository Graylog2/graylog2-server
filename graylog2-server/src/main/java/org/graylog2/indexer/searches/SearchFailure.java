package org.graylog2.indexer.searches;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SearchFailure {

    final private List<String> errors;

    public SearchFailure(JsonNode shards) {
        errors = StreamSupport.stream(shards.path("failures").spliterator(), false)
                .map(failure -> {
                    final String error = failure.path("reason").path("reason").asText();
                    final String caused_by = failure.path("reason").path("caused_by").toString();
                    if (!caused_by.isEmpty()) {
                        return error + " caused_by: " + caused_by;
                    }
                    return error;
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getNonNumericFieldErrors() {
        return errors.stream().filter(error ->
                error.startsWith("Expected numeric type on field") ||
                error.contains("\"type\":\"number_format_exception")).
                collect(Collectors.toList());
    }
}
