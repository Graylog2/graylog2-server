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
package org.graylog.collectors.config.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.auto.value.AutoValue;
import org.graylog.collectors.config.CollectorAttributes;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

/**
 * Configuration for the OpenTelemetry resource processor.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/processor/resourceprocessor/README.md">Processor Documentation</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class ResourceProcessorConfig implements CollectorProcessorConfig {
    @JsonProperty("attributes")
    public abstract List<Attribute> attributes();

    public static Builder builder(String id) {
        return new AutoValue_ResourceProcessorConfig.Builder()
                .id(id)
                .name(f("resource/%s", id));
    }

    public static Attribute collectorComponentAttribute(String compoenent) {
        return Attribute.upsert(CollectorAttributes.COLLECTOR_RECEIVER_TYPE, compoenent);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder name(String name);

        public abstract Builder attributes(List<Attribute> attributes);

        public abstract ResourceProcessorConfig build();
    }

    public enum Action {
        @JsonProperty("upsert")
        UPSERT
    }

    public record Attribute(@JsonProperty("key") String key,
                            @JsonProperty("value") ValueNode value,
                            @JsonProperty("action") Action action) {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        public Attribute {
            requireNonBlank(key, "name can't be blank");
            requireNonNull(value, "value can't be null");
        }

        public static Attribute upsert(String name, String value) {
            return new Attribute(name, MAPPER.convertValue(value, ValueNode.class), Action.UPSERT);
        }

        public static Attribute upsert(String name, Number value) {
            return new Attribute(name, MAPPER.convertValue(value, ValueNode.class), Action.UPSERT);
        }

        public static Attribute upsert(String name, boolean value) {
            return new Attribute(name, MAPPER.convertValue(value, ValueNode.class), Action.UPSERT);
        }
    }
}
