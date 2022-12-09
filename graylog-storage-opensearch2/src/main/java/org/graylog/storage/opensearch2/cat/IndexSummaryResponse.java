package org.graylog.storage.opensearch2.cat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexSummaryResponse(@JsonProperty("index") String index,
                                   @JsonProperty("status") String status,
                                   @JsonProperty("health") String health) {
}
