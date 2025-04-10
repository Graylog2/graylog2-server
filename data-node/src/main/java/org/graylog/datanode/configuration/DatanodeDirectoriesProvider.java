package org.graylog.datanode.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class DatanodeDirectoriesProvider implements Provider<DatanodeDirectories> {

    private final DatanodeDirectories directories;

    @Inject
    public DatanodeDirectoriesProvider(DatanodeConfiguration datanodeConfiguration) {
        this.directories = datanodeConfiguration.datanodeDirectories();
    }

    @Override
    public DatanodeDirectories get() {
        return directories;
    }
}
