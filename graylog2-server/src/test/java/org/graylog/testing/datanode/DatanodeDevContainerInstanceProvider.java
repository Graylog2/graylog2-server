package org.graylog.testing.datanode;

import org.graylog2.storage.SearchVersion;

import java.util.Optional;
import java.util.ServiceLoader;

public class DatanodeDevContainerInstanceProvider {
    private static ServiceLoader<DatanodeDevContainerInterfaceProvider> loader = ServiceLoader.load(DatanodeDevContainerInterfaceProvider.class);

    public static Optional<DatanodeDevContainerBuilder> getBuilderFor(SearchVersion searchVersion) {
        for (DatanodeDevContainerInterfaceProvider provider : loader) {
            DatanodeDevContainerBuilder container = provider.getBuilderFor(searchVersion);
            if (container != null) {
                return Optional.of(container);
            }
        }
        return Optional.empty();
    }
}
