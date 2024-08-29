package org.graylog2.rest.models.system.indices;

import java.util.Optional;

public interface SnapshotService {
    /**
     * Optionally return name of failed snapshot index
     *
     * @param indexSetId ID of an index set
     * @return index name or empty
     */
    Optional<String> getFailedSnapshot(String indexSetId);
}
