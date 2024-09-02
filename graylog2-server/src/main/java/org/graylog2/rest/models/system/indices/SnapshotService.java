package org.graylog2.rest.models.system.indices;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;

import java.util.Optional;

public interface SnapshotService {
    /**
     * Optionally return name of failed snapshot index
     *
     * @param indexSetId ID of an index set
     * @return index name or empty
     */
    Optional<String> getFailedSnapshotName(IndexSet indexSet, IndexSetConfig indexSetConfig);

    void deleteSnapshot(IndexSet indexSet, IndexSetConfig indexSetConfig);
}
