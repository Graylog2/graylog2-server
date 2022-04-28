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
package org.graylog.metrics.prometheus.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;

import java.util.Collection;

public interface MetricMapping {
    Collection<MapperConfig> toMapperConfigs();

    interface Factory<TYPE extends MetricMapping> {
        TYPE create(Config config);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MetricMatchMapping.Config.class, name = MetricMatchMapping.TYPE),
            @JsonSubTypes.Type(value = InputMetricMapping.Config.class, name = InputMetricMapping.TYPE),
    })
    interface Config {
        @JsonProperty("type")
        String type();

        @JsonProperty("metric_name")
        String metricName();

        interface Builder<SELF> {
            @JsonProperty("type")
            SELF type(String type);

            @JsonProperty("metric_name")
            SELF metricName(String metricName);
        }
    }
}
