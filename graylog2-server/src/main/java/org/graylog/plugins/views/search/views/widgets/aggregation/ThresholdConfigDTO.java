package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public record ThresholdConfigDTO(@JsonProperty @NotBlank String color,
                                 @JsonProperty Optional<String> name,
                                 @JsonProperty double value) {
}
