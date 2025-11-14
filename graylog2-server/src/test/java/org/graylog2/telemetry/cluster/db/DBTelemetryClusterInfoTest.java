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
package org.graylog2.telemetry.cluster.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DBTelemetryClusterInfoTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private DBTelemetryClusterInfo dbTelemetryClusterInfo;

    @Before
    public void setUp() {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final MongoCollections mongoCollections = new MongoCollections(mapperProvider, mongodb.mongoConnection());
        dbTelemetryClusterInfo = new DBTelemetryClusterInfo(Duration.ofMinutes(5), mongoCollections);
    }

    @Test
    public void findAllReturnsEmptyListWhenNoDocuments() {
        final List<TelemetryClusterInfoDto> all = dbTelemetryClusterInfo.findAll();

        assertThat(all).isEmpty();
    }

    @Test
    public void findAllReturnsAllDocuments() {
        final TelemetryClusterInfoDto dto1 = nodeInfo("node-1", true);
        final TelemetryClusterInfoDto dto2 = nodeInfo("node-2", false);

        dbTelemetryClusterInfo.update(dto1);
        dbTelemetryClusterInfo.update(dto2);

        final List<TelemetryClusterInfoDto> nodeInfoList = dbTelemetryClusterInfo.findAll();

        assertThat(nodeInfoList).hasSize(2);
        assertThat(nodeInfoList)
                .extracting(TelemetryClusterInfoDto::nodeId)
                .containsExactlyInAnyOrder("node-1", "node-2");
    }

    @Test
    public void updateInsertsNewDocument() {
        final TelemetryClusterInfoDto dto = nodeInfo("node-1", true);

        dbTelemetryClusterInfo.update(dto);

        final List<TelemetryClusterInfoDto> nodeInfoList = dbTelemetryClusterInfo.findAll();
        final TelemetryClusterInfoDto nodeInfo = nodeInfoList.getFirst();

        assertThat(nodeInfoList).hasSize(1);
        assertThat(nodeInfo.id()).isNotNull();
        assertThat(nodeInfo.nodeId()).isEqualTo(dto.nodeId());
        assertThat(nodeInfo.clusterId()).isEqualTo(dto.clusterId());
        assertThat(nodeInfo.codename()).isEqualTo(dto.codename());
        assertThat(nodeInfo.facility()).isEqualTo(dto.facility());
        assertThat(nodeInfo.hostname()).isEqualTo(dto.hostname());
        assertThat(nodeInfo.isLeader()).isEqualTo(dto.isLeader());
        assertThat(nodeInfo.isProcessing()).isEqualTo(dto.isProcessing());
        assertThat(nodeInfo.lbStatus()).isEqualTo(dto.lbStatus());
        assertThat(nodeInfo.lifecycle()).isEqualTo(dto.lifecycle());
        assertThat(nodeInfo.operatingSystem()).isEqualTo(dto.operatingSystem());
        assertThat(nodeInfo.startedAt()).isEqualTo(dto.startedAt());
        assertThat(nodeInfo.timezone()).isEqualTo(dto.timezone());
        assertThat(nodeInfo.updatedAt()).isNotNull();
        assertThat(nodeInfo.version()).isEqualTo(dto.version());
    }

    @Test
    public void updateOverwritesExistingDocumentForSameNode() {
        final TelemetryClusterInfoDto dto1 = nodeInfo("node-1", true);
        dbTelemetryClusterInfo.update(dto1);

        final TelemetryClusterInfoDto dto2 = nodeInfo("node-1", false);
        dbTelemetryClusterInfo.update(dto2);

        final List<TelemetryClusterInfoDto> nodeInfoList = dbTelemetryClusterInfo.findAll();
        final TelemetryClusterInfoDto nodeInfo = nodeInfoList.getFirst();

        assertThat(nodeInfoList).hasSize(1);
        assertThat(nodeInfo.nodeId()).isEqualTo(dto2.nodeId());
        assertThat(nodeInfo.clusterId()).isEqualTo(dto2.clusterId());
        assertThat(nodeInfo.codename()).isEqualTo(dto2.codename());
        assertThat(nodeInfo.facility()).isEqualTo(dto2.facility());
        assertThat(nodeInfo.hostname()).isEqualTo(dto2.hostname());
        assertThat(nodeInfo.isLeader()).isEqualTo(dto2.isLeader());
        assertThat(nodeInfo.isProcessing()).isEqualTo(dto2.isProcessing());
        assertThat(nodeInfo.lbStatus()).isEqualTo(dto2.lbStatus());
        assertThat(nodeInfo.lifecycle()).isEqualTo(dto2.lifecycle());
        assertThat(nodeInfo.operatingSystem()).isEqualTo(dto2.operatingSystem());
        assertThat(nodeInfo.startedAt()).isEqualTo(dto2.startedAt());
        assertThat(nodeInfo.timezone()).isEqualTo(dto2.timezone());
        assertThat(nodeInfo.updatedAt()).isNotNull();
        assertThat(nodeInfo.version()).isEqualTo(dto2.version());
    }

    private TelemetryClusterInfoDto nodeInfo(String nodeId, boolean isLeader) {
        final String randStr = UUID.randomUUID().toString();

        return TelemetryClusterInfoDto.Builder.create()
                .nodeId(nodeId)
                .isLeader(isLeader)
                .clusterId("cluster-1" + randStr)
                .codename("Noir-" + randStr)
                .facility("graylog-server-" + randStr)
                .hostname("hostname-" + randStr)
                .isProcessing(true)
                .lbStatus("active")
                .lifecycle("running")
                .operatingSystem("Linux 5.4-" + randStr)
                .startedAt(DateTime.now(DateTimeZone.UTC).minusDays(5))
                .timezone("UTC-" + randStr)
                .version("5.2.0-" + randStr)
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
    }
}
