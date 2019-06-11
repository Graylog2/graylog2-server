package org.graylog.plugins.enterprise.search.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.enterprise.search.Query;

public class UnboundParameterError extends QueryError {
    private final String parameterName;

    public UnboundParameterError(Query query, String name) {
        super(query, "Unbound required parameter used: " + name);
        this.parameterName = name;
    }

    @JsonProperty("parameter")
    public String parameterName() {
        return parameterName;
    }
}
