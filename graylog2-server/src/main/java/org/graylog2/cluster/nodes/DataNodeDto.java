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
import jakarta.annotation.Nullable;
import org.graylog.security.certutil.CertRenewalService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.plugin.Version;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AutoValue
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = DataNodeDto.class)
@JsonDeserialize(builder = DataNodeDto.Builder.class)
public abstract class DataNodeDto extends NodeDto {

    public static final String FIELD_CERT_VALID_UNTIL = "cert_valid_until";
    public static final String FIELD_DATANODE_VERSION = "datanode_version";

    @Nullable
    @JsonProperty("cluster_address")
    public abstract String getClusterAddress();

    @Nullable
    @JsonProperty("rest_api_address")
    public abstract String getRestApiAddress();

    @JsonProperty("data_node_status")
    public abstract DataNodeStatus getDataNodeStatus();

    @Nullable
    @JsonProperty("action_queue")
    public abstract DataNodeLifecycleTrigger getActionQueue();

    @Nullable
    @JsonProperty(FIELD_CERT_VALID_UNTIL)
    public abstract Date getCertValidUntil();

    @Nullable
    @JsonProperty(FIELD_DATANODE_VERSION)
    public abstract String getDatanodeVersion();

    @JsonProperty("version_compatible")
    public boolean isCompatibleWithVersion() {
        return Optional.ofNullable(getDatanodeVersion())
                .map(datanodeVersion -> isVersionEqualIgnoreBuildMetadata(datanodeVersion, Version.CURRENT_CLASSPATH))
                .orElse(false);
    }

    protected static boolean isVersionEqualIgnoreBuildMetadata(String datanodeVersion, Version serverVersion) {
        final com.github.zafarkhaja.semver.Version datanode = com.github.zafarkhaja.semver.Version.parse(datanodeVersion);
        return serverVersion.getVersion().compareToIgnoreBuildMetadata(datanode) == 0;
    }

    @Nullable
    @JsonUnwrapped
    public CertRenewalService.ProvisioningInformation getProvisioningInformation() {
        DataNodeProvisioningConfig.State state = switch (getDataNodeStatus()) {
            case AVAILABLE -> DataNodeProvisioningConfig.State.CONNECTED;
            case STARTING -> DataNodeProvisioningConfig.State.STARTING;
            case PREPARED -> DataNodeProvisioningConfig.State.PROVISIONED;
            default -> DataNodeProvisioningConfig.State.UNCONFIGURED;
        };

        final LocalDateTime certValidTill = Optional.ofNullable(getCertValidUntil())
                .map(date -> Instant.ofEpochMilli(date.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .orElse(null);

        return new CertRenewalService.ProvisioningInformation(state, null, certValidTill);
    }

    @Override
    public Map<String, Object> toEntityParameters() {
        final Map<String, Object> params = super.toEntityParameters();
        if (Objects.nonNull(getClusterAddress())) {
            params.put("cluster_address", getClusterAddress());
        }
        if (Objects.nonNull(getRestApiAddress())) {
            params.put("rest_api_address", getRestApiAddress());
        }
        if (Objects.nonNull(getDataNodeStatus())) {
            params.put("datanode_status", getDataNodeStatus());
        }
        if (Objects.nonNull(getActionQueue())) {
            if (getActionQueue() == DataNodeLifecycleTrigger.CLEAR) {
                params.put("action_queue", null);
            } else {
                params.put("action_queue", getActionQueue());
            }
        }

        if (Objects.nonNull(getCertValidUntil())) {
            params.put(FIELD_CERT_VALID_UNTIL, getCertValidUntil());
        }

        if(Objects.nonNull(getDatanodeVersion())) {
            params.put(FIELD_DATANODE_VERSION, getDatanodeVersion());
        }

        return params;
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder extends NodeDto.Builder<Builder> {

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_DataNodeDto.Builder()
                    .setLeader(false); // TODO: completely remove the leader property from this DTO
        }

        @JsonProperty("cluster_address")
        public abstract Builder setClusterAddress(String clusterAddress);

        @JsonProperty("rest_api_address")
        public abstract Builder setRestApiAddress(String restApiAddress);

        @JsonProperty("datanode_status")
        public abstract Builder setDataNodeStatus(DataNodeStatus dataNodeStatus);

        @JsonProperty("action_queue")
        public abstract Builder setActionQueue(DataNodeLifecycleTrigger trigger);

        @JsonProperty(FIELD_CERT_VALID_UNTIL)
        public abstract Builder setCertValidUntil(Date certValidUntil);

        @JsonProperty(FIELD_DATANODE_VERSION)
        public abstract Builder setDatanodeVersion(String datanodeVersion);

        public abstract DataNodeDto build();
    }
}
