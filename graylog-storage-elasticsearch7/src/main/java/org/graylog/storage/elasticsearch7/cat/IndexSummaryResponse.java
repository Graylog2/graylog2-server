package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexSummaryResponse(@JsonProperty("index") String index,
                                   @JsonProperty("status") String status,
                                   @JsonProperty("health") String health) {
}
