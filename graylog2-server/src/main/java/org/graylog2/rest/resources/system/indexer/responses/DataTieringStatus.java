package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataTieringStatus(
        @JsonProperty("has_failed_snapshot") boolean hasFailedSnapshot,
        @JsonProperty("failed_snapshot_name") String failedSnapshotName
) {
}
