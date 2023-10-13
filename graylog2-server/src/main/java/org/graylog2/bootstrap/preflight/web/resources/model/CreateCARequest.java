package org.graylog2.bootstrap.preflight.web.resources.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateCARequest(@JsonProperty("organization") String organization) {
}
