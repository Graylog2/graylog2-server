package org.graylog2.rest.models.system.indices;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;

import java.util.Optional;

public class SnapshotServiceNoop implements SnapshotService {

    @Override
    public Optional<String> getFailedSnapshotName(IndexSet indexSet, IndexSetConfig indexSetConfig) {
        return Optional.empty();
    }

    @Override
    public void deleteSnapshot(IndexSet indexSet, IndexSetConfig indexSetConfig) {
        // never called
    }

}
