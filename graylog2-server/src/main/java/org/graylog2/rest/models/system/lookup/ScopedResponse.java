package org.graylog2.rest.models.system.lookup;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

public interface ScopedResponse {
    String FIELD_SCOPE = "scope";

    String scope();
}
