package org.graylog.testing.datanode;

import org.graylog2.storage.SearchVersion;

public interface DatanodeDevContainerInterfaceProvider {
    DatanodeDevContainerBuilder getBuilderFor(SearchVersion version);
}
