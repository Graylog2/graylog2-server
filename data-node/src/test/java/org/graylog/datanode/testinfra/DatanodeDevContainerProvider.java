package org.graylog.datanode.testinfra;

import org.graylog.testing.datanode.DatanodeDevContainerInterfaceProvider;
import org.graylog2.storage.SearchVersion;

public class DatanodeDevContainerProvider implements DatanodeDevContainerInterfaceProvider {
    @Override
    public DatanodeDevContainerBuilder getBuilderFor(SearchVersion version) {
        return new DatanodeDevContainerBuilder();
    }
}
