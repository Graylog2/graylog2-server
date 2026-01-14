package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StreamReference(
        @JsonProperty("id") String id,
        @JsonProperty("title") String title
) {}
