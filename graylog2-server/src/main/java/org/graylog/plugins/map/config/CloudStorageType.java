/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
