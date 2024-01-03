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
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.graylog.security.certutil.CertRenewalService;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = DataNodeDto.class)
@JsonDeserialize(builder = DataNodeDto.Builder.class)
public abstract class DataNodeDto extends NodeDto {

    @Nullable
    @JsonProperty("cluster_address")
    public abstract String getClusterAddress();

    @Nullable
    @JsonProperty("rest_api_address")
    public abstract String getRestApiAddress();

    @JsonProperty("data_node_status")
    public abstract DataNodeStatus getDataNodeStatus();

    @Nullable
    @JsonUnwrapped
    public abstract CertRenewalService.ProvisioningInformation getProvisioningInformation();

    @Override
    public Map<String, Object> toEntityParameters() {
        final Map<String, Object> params = super.toEntityParameters();
        params.put("cluster_address", getClusterAddress());
        params.put("rest_api_address", getRestApiAddress());
        params.put("datanode_status", getDataNodeStatus());
        return params;
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder extends NodeDto.Builder<Builder> {

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_DataNodeDto.Builder();
        }

        @JsonProperty("cluster_address")
        public abstract Builder setClusterAddress(String clusterAddress);

        @JsonProperty("rest_api_address")
        public abstract Builder setRestApiAddress(String restApiAddress);

        @JsonProperty("datanode_status")
        public abstract Builder setDataNodeStatus(DataNodeStatus dataNodeStatus);

        public abstract Builder setProvisioningInformation(CertRenewalService.ProvisioningInformation provisioningInformation);

        public abstract DataNodeDto build();


    }
}
