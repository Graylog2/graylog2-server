package org.graylog.plugins.map.config;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tells in which cloud-storage the files for the GeoIP database are stored.
 */
public enum CloudStorageType {
    S3("s3"),
    GCS("gcs");

    private final String name;

    CloudStorageType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public static CloudStorageType fromName(String name) {
        return switch (name) {
            case "s3" -> S3;
            case "gcs" -> GCS;
            default -> throw new IllegalArgumentException("Unknown cloud storage: " + name);
        };
    }
}
