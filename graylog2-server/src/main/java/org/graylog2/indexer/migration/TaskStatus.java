package org.graylog2.indexer.migration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskStatus(
        @JsonProperty("total") long total,
        @JsonProperty("updated") long updated,
        @JsonProperty("created") long created,
        @JsonProperty("deleted") long deleted,
        @JsonProperty("batches") long batches,
        @JsonProperty("version_conflicts") long versionConflicts,
        @JsonProperty("noops") long noops,
        @JsonProperty("failures") List<String> failures

) {

    public static TaskStatus unknown() {
        return new TaskStatus(-1, -1, -1, -1, -1, -1, -1, Collections.emptyList());
    }
}
