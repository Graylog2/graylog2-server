package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public record QueryStringUsedDTO(@JsonProperty("query_string") @NotNull @NotEmpty String queryString) {
}
