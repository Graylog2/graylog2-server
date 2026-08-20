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

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.CertRenewalService;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.graylog2.plugin.Version;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class DataNodeDtoTest {

    @Test
    void testVersionCompatibility() {
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0-SNAPSHOT+9bec368", classpathVersion("6.2.0-SNAPSHOT")))
                .isTrue();
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0-SNAPSHOT+9bec368", classpathVersion("6.2.0-SNAPSHOT+9bec369")))
                .isTrue();
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0-SNAPSHOT+9bec368", classpathVersion("6.2.0-SNAPSHOT+9bec368")))
                .isTrue();

        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.2.0", classpathVersion("5.1.0")))
                .isFalse();
        Assertions.assertThat(DataNodeDto.isVersionEqualIgnoreBuildMetadata("6.1.0-SNAPSHOT+1bec368", classpathVersion("6.2.0-SNAPSHOT")))
                .isFalse();
    }

    @Test
    void testIsCompatibleWithVersion() {
        final String currentVersion = Version.CURRENT_CLASSPATH.getVersion().toString();

        assertThat(dto(b -> b.setDatanodeVersion(currentVersion)).isCompatibleWithVersion()).isTrue();
        assertThat(dto(b -> b.setDatanodeVersion("0.0.1")).isCompatibleWithVersion()).isFalse();
        assertThat(dto(b -> b).isCompatibleWithVersion()).isFalse();
    }

    @Test
    void testGetProvisioningInformation() {
        assertThat(provisioningState(DataNodeStatus.AVAILABLE)).isEqualTo(DataNodeProvisioningConfig.State.CONNECTED);
        assertThat(provisioningState(DataNodeStatus.STARTING)).isEqualTo(DataNodeProvisioningConfig.State.STARTING);
        assertThat(provisioningState(DataNodeStatus.PREPARED)).isEqualTo(DataNodeProvisioningConfig.State.PROVISIONED);
        assertThat(provisioningState(DataNodeStatus.UNCONFIGURED)).isEqualTo(DataNodeProvisioningConfig.State.UNCONFIGURED);
        assertThat(provisioningState(DataNodeStatus.UNAVAILABLE)).isEqualTo(DataNodeProvisioningConfig.State.UNCONFIGURED);
        assertThat(provisioningState(DataNodeStatus.REMOVING)).isEqualTo(DataNodeProvisioningConfig.State.UNCONFIGURED);
        assertThat(provisioningState(DataNodeStatus.REMOVED)).isEqualTo(DataNodeProvisioningConfig.State.UNCONFIGURED);
    }

    @Test
    void testGetProvisioningInformationCertValidUntil() {
        final Date certDate = new Date(1_000_000_000L);

        final CertRenewalService.ProvisioningInformation info = dto(b -> b
                .setDataNodeStatus(DataNodeStatus.AVAILABLE)
                .setCertValidUntil(certDate)
        ).getProvisioningInformation();

        assertThat(info.certValidUntil()).isNotNull().isInstanceOf(LocalDateTime.class);

        assertThat(dto(b -> b.setDataNodeStatus(DataNodeStatus.AVAILABLE)).getProvisioningInformation().certValidUntil())
                .isNull();
    }

    @Test
    void testToEntityParameters() {
        final Map<String, Object> params = dto(b -> b
                .setClusterAddress("http://cluster:9300")
                .setRestApiAddress("http://rest:9200")
                .setDatanodeVersion("6.0.0")
                .setActionQueue(DataNodeLifecycleTrigger.STOP)
        ).toEntityParameters();

        assertThat(params).containsEntry("node_id", "test-node-id")
                .containsEntry("is_leader", false)
                .containsEntry(DataNodeDto.FIELD_CLUSTER_ADDRESS, "http://cluster:9300")
                .containsEntry(DataNodeDto.FIELD_REST_API_ADDRESS, "http://rest:9200")
                .containsEntry(DataNodeDto.FIELD_DATANODE_STATUS, DataNodeStatus.STARTING)
                .containsEntry(DataNodeDto.FIELD_DATANODE_VERSION, "6.0.0")
                .containsEntry(DataNodeDto.FIELD_ACTION_QUEUE, DataNodeLifecycleTrigger.STOP);
    }

    @Test
    void testToEntityParametersClearActionQueue() {
        final Map<String, Object> params = dto(b -> b.setActionQueue(DataNodeLifecycleTrigger.CLEAR)).toEntityParameters();

        assertThat(params).containsKey(DataNodeDto.FIELD_ACTION_QUEUE);
        assertThat(params.get(DataNodeDto.FIELD_ACTION_QUEUE)).isNull();
    }

    @Test
    void testToEntityParametersOmitsNullOptionalFields() {
        final Map<String, Object> params = dto(b -> b).toEntityParameters();

        assertThat(params).doesNotContainKeys(
                DataNodeDto.FIELD_CLUSTER_ADDRESS, DataNodeDto.FIELD_REST_API_ADDRESS,
                DataNodeDto.FIELD_ACTION_QUEUE, DataNodeDto.FIELD_CERT_VALID_UNTIL,
                DataNodeDto.FIELD_DATANODE_VERSION, DataNodeDto.FIELD_OPENSEARCH_ROLES,
                DataNodeDto.FIELD_CONFIGURATION_WARNINGS);
    }

    private DataNodeProvisioningConfig.State provisioningState(DataNodeStatus status) {
        return dto(b -> b.setDataNodeStatus(status)).getProvisioningInformation().status();
    }

    private DataNodeDto dto(UnaryOperator<DataNodeDto.Builder> customizer) {
        final DataNodeDto.Builder base = DataNodeDto.Builder.builder()
                .setId("test-node-id")
                .setDataNodeStatus(DataNodeStatus.STARTING);
        return customizer.apply(base).build();
    }

    @Nonnull
    private static Version classpathVersion(String version) {
        return new Version(com.github.zafarkhaja.semver.Version.parse(version));
    }
}
