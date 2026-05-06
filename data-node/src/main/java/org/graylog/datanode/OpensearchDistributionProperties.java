package org.graylog.datanode;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * These are typed keys to the opense
 */
public class OpensearchDistributionProperties {
    private final Properties properties;

    public static OpensearchDistributionProperties forVersion(@NotNull String version) {
        try (
                final InputStream stream = OpensearchDistributionProperties.class.getResourceAsStream(Path.of("/", "opensearch", "config", version, "distribution.properties").toString())
        ) {
            Properties properties = new Properties();
            properties.load(stream);
            return new OpensearchDistributionProperties(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OpensearchDistributionProperties(Properties properties) {
        this.properties = properties;
    }

    public String searchableSnapshotsRole() {
        return getProperty("searchable_snapshots_role");
    }

    private <T> T getProperty(String name) {
        if (properties.containsKey(name)) {
            return (T) properties.getProperty(name);
        } else {
            throw new IllegalArgumentException(String.format("Opensearch distribution property '%s' not found", name));
        }
    }
}
