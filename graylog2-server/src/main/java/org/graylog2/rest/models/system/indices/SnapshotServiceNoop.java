package org.graylog2.rest.models.system.indices;

import java.util.Optional;

public class SnapshotServiceNoop implements SnapshotService {

    @Override
    public Optional<String> getFailedSnapshot(String indexSetId) {
        return Optional.empty();
    }

}
