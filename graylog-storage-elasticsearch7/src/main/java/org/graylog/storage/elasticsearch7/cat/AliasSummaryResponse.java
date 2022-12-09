package org.graylog.storage.elasticsearch7.cat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AliasSummaryResponse(@JsonProperty("alias") String alias,
                                   @JsonProperty("index") String index) {
}
