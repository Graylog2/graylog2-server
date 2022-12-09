package org.graylog.storage.opensearch2.cat;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AliasSummaryResponse(@JsonProperty("alias") String alias,
                                   @JsonProperty("index") String index) {
}
