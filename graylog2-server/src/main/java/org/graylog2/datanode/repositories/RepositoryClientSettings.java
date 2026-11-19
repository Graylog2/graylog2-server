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
package org.graylog2.datanode.repositories;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.security.encryption.EncryptedValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * The settings of an opensearch repository client, as Data Nodes apply them. Keys are relative to the client, e.g.
 * {@code endpoint} becomes {@code s3.client.<client name>.endpoint}.
 *
 * @param type           the repository type, which is the prefix of the opensearch client settings
 * @param settings       regular settings, they end up in the opensearch configuration file
 * @param secureSettings secure settings, they end up in the opensearch keystore
 * @param secureFiles    secure settings whose value is the content of a file, like a credentials file, they end up
 *                       in the opensearch keystore
 */
public record RepositoryClientSettings(@JsonProperty("type") String type,
                                       @JsonProperty("settings") Map<String, String> settings,
                                       @JsonProperty("secure_settings") Map<String, EncryptedValue> secureSettings,
                                       @JsonProperty("secure_files") Map<String, EncryptedValue> secureFiles) {

    public static final String TYPE_S3 = "s3";
    public static final String TYPE_GCS = "gcs";
    public static final String TYPE_AZURE = "azure";

    private static final Set<String> TYPES = Set.of(TYPE_S3, TYPE_GCS, TYPE_AZURE);
    // Setting keys are part of opensearch setting paths, a dot would address another setting
    private static final Pattern VALID_KEY = Pattern.compile("[a-z][a-z0-9_]*");

    public RepositoryClientSettings {
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException(f("Unsupported repository client type <%s>", type));
        }
        settings = Map.copyOf(Objects.requireNonNullElse(settings, Map.of()));
        secureSettings = Map.copyOf(Objects.requireNonNullElse(secureSettings, Map.of()));
        secureFiles = Map.copyOf(Objects.requireNonNullElse(secureFiles, Map.of()));
        for (Map<String, ?> keys : List.of(settings, secureSettings, secureFiles)) {
            keys.keySet().stream()
                    .filter(key -> !VALID_KEY.matcher(key).matches())
                    .findFirst()
                    .ifPresent(key -> {
                        throw new IllegalArgumentException(f("Invalid repository client setting <%s>", key));
                    });
        }
    }
}
