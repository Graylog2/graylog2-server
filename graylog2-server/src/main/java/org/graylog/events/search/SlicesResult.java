package org.graylog.events.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SlicesResult(@JsonProperty(FIELD_SLICES) List<Slice> slices) {
    private static final String FIELD_SLICES = "slices";
}
