package org.graylog.plugins.views.search.views.formatting.highlighting;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Condition {
    @JsonProperty("equal")
    EQUAL,
    @JsonProperty("not_equal")
    NOT_EQUAL,
    @JsonProperty("greater")
    GREATER,
    @JsonProperty("greater_equal")
    GREATER_EQUAL,
    @JsonProperty("less")
    LESS,
    @JsonProperty("less_equal")
    LESS_EQUAL,
}
