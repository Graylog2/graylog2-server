package org.graylog.datanode.configuration;

import com.github.joschi.jadconfig.Parameter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathConfiguration {
    protected static final Path DEFAULT_DATA_DIR = Paths.get("data");

    @Parameter(value = "data_dir", required = true)
    private Path dataDir = DEFAULT_DATA_DIR;

    public Path getNativeLibDir() {
        return dataDir.resolve("libnative");
    }
}
