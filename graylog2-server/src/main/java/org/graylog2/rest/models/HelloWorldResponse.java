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
package org.graylog2.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class HelloWorldResponse {
    @JsonProperty("cluster_id")
    public abstract String clusterId();

    @JsonProperty("node_id")
    public abstract String nodeId();

    @JsonProperty("version")
    public abstract String version();

    @JsonProperty("tagline")
    public abstract String tagline();

    public static HelloWorldResponse create(String clusterId, String nodeId, String version, String tagline) {
        return new AutoValue_HelloWorldResponse(clusterId, nodeId, version, tagline);
    }
}
