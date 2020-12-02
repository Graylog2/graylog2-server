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
package org.graylog2.rest.models.system.codecs.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.configuration.ConfigurationRequest;

import java.util.Map;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class CodecTypeInfo {
    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract Map<String, Map<String, Object>> requestedConfiguration();

    @JsonCreator
    public static CodecTypeInfo create(@JsonProperty("type") String type,
                                       @JsonProperty("name") String name,
                                       @JsonProperty("requested_configuration") Map<String, Map<String, Object>> requestedConfiguration) {
        return new AutoValue_CodecTypeInfo(type, name, requestedConfiguration);
    }

    public static CodecTypeInfo fromConfigurationRequest(String type, String name, ConfigurationRequest configurationRequest) {
        return create(type, name, configurationRequest.asList());
    }
}
