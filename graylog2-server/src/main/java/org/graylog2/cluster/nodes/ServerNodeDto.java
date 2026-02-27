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
package org.graylog2.cluster.nodes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.lifecycles.LoadBalancerStatus;

import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = ServerNodeDto.class)
@JsonDeserialize(builder = ServerNodeDto.Builder.class)
public abstract class ServerNodeDto extends NodeDto {

    public static final String FIELD_IS_PROCESSING = "is_processing";
    public static final String FIELD_LOAD_BALANCER_STATUS = "lb_status";
    public static final String FIELD_LIFECYCLE = "lifecycle";

    @JsonProperty(FIELD_IS_PROCESSING)
    public abstract boolean isProcessing();

    @Nullable
    @JsonProperty(FIELD_LOAD_BALANCER_STATUS)
    public LoadBalancerStatus getLoadBalancerStatus() {
        return Optional.ofNullable(getLifecycle())
                .map(Lifecycle::getLoadbalancerStatus)
                .orElse(null);
    }

    @Nullable
    @JsonProperty(FIELD_LIFECYCLE)
    public abstract Lifecycle getLifecycle();

    public abstract Builder toBuilder();

    @Override
    public Map<String, Object> toEntityParameters() {
        final Map<String, Object> entityParameters = super.toEntityParameters();
        entityParameters.put(FIELD_IS_PROCESSING, isProcessing());
        if(getLifecycle() != null) {
            entityParameters.put(FIELD_LIFECYCLE, getLifecycle().name());
        }
        return entityParameters;
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder extends NodeDto.Builder<Builder> {

        @JsonProperty(FIELD_IS_PROCESSING)
        public abstract ServerNodeDto.Builder setProcessing(boolean isProcessing);

        @JsonProperty(FIELD_LIFECYCLE)
        public abstract Builder setLifecycle(@Nullable Lifecycle lifecycle);

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_ServerNodeDto.Builder();
        }

        public abstract ServerNodeDto build();
    }
}
