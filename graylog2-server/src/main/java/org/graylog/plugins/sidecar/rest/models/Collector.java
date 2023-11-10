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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

@AutoValue
@JsonAutoDetect
public abstract class Collector {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SERVICE_TYPE = "service_type";
    public static final String FIELD_NODE_OPERATING_SYSTEM = "node_operating_system";
    public static final String FIELD_EXECUTABLE_PATH = "executable_path";
    public static final String FIELD_EXECUTE_PARAMETERS = "execute_parameters";
    public static final String FIELD_VALIDATION_PARAMETERS = "validation_parameters";
    public static final String FIELD_DEFAULT_TEMPLATE = "default_template";
    public static final String FIELD_DEFAULT_TEMPLATE_CRC = "default_template_crc";

    // Set of prior version CRCs for back-compat
    private static final Set<Long> INITIAL_CRC = ImmutableSet.of(
            3280545580L, // 5.2 filebeat linux
            3396210381L, // 5.2 filebeat darwin
            3013497446L, // 5.2 filebeat freebsd
            4009863009L, // 5.2 winlogbeat windows
            2023247173L, // 5.2 nxlog linux
            2491201449L, // 5.2 nxlog windows
            2487909285L  // 5.2 auditbeat windows
    );

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    // exec, svc, systemd, ...
    @JsonProperty(FIELD_SERVICE_TYPE)
    public abstract String serviceType();

    @JsonProperty(FIELD_NODE_OPERATING_SYSTEM)
    public abstract String nodeOperatingSystem();

    @JsonProperty(FIELD_EXECUTABLE_PATH)
    public abstract String executablePath();

    @JsonProperty(FIELD_EXECUTE_PARAMETERS)
    @Nullable
    public abstract String executeParameters();

    @JsonProperty(FIELD_VALIDATION_PARAMETERS)
    @Nullable
    public abstract String validationParameters();

    @JsonProperty(FIELD_DEFAULT_TEMPLATE)
    @Nullable
    public abstract String defaultTemplate();

    @JsonProperty(FIELD_DEFAULT_TEMPLATE_CRC)
    @Nullable
    public abstract Long defaultTemplateCRC();

    @JsonIgnore
    public boolean defaultTemplateUpdated() {
        long crc = checksum(defaultTemplate().getBytes(StandardCharsets.UTF_8));
        if (defaultTemplateCRC() == null) {
            return (!INITIAL_CRC.contains(crc));
        }
        return (crc != defaultTemplateCRC());
    }

    @JsonIgnore
    public static long checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    public static Builder builder() {
        return new AutoValue_Collector.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);
        public abstract Builder name(String value);
        public abstract Builder serviceType(String serviceType);
        public abstract Builder nodeOperatingSystem(String nodeOperatingSystem);
        public abstract Builder executablePath(String executablePath);
        public abstract Builder executeParameters(String executeParameters);
        public abstract Builder validationParameters(String validationParameters);
        public abstract Builder defaultTemplate(String defaultTemplate);

        public abstract Builder defaultTemplateCRC(Long checksum);
        public abstract Collector build();
    }

    @JsonCreator
    public static Collector create(@JsonProperty(FIELD_ID) @Nullable String id,
                                   @JsonProperty(FIELD_NAME) String name,
                                   @JsonProperty(FIELD_SERVICE_TYPE) String serviceType,
                                   @JsonProperty(FIELD_NODE_OPERATING_SYSTEM) String nodeOperatingSystem,
                                   @JsonProperty(FIELD_EXECUTABLE_PATH) String executablePath,
                                   @JsonProperty(FIELD_EXECUTE_PARAMETERS) @Nullable String executeParameters,
                                   @JsonProperty(FIELD_VALIDATION_PARAMETERS) @Nullable String validationParameters,
                                   @JsonProperty(FIELD_DEFAULT_TEMPLATE) @Nullable String defaultTemplate,
                                   @JsonProperty(FIELD_DEFAULT_TEMPLATE_CRC) @Nullable Long defaultTemplateCRC) {
        return builder()
                .id(id)
                .name(name)
                .serviceType(serviceType)
                .nodeOperatingSystem(nodeOperatingSystem)
                .executablePath(executablePath)
                .executeParameters(executeParameters)
                .validationParameters(validationParameters)
                .defaultTemplate(defaultTemplate)
                .defaultTemplateCRC(defaultTemplateCRC)
                .build();
    }
}
