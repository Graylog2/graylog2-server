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
package org.graylog2.rest.resources.system.debug.bundle;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.shiro.subject.Subject;
import org.graylog.plugins.pipelineprocessor.rest.PipelineLoad;
import org.graylog.plugins.pipelineprocessor.rest.ProcessingLoadResponse;
import org.graylog.plugins.pipelineprocessor.rest.ProcessingLoadBuilder;
import org.graylog.plugins.pipelineprocessor.rest.RuleLoad;
import org.graylog.plugins.pipelineprocessor.rest.StageRuleLoad;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.TestNodeService;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.system.stats.ClusterStats;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.datanodes.DatanodeRestApiProxy;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.versionprobe.VersionProbeFactory;
import org.graylog2.system.stats.ClusterStatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupportBundleServiceIT {

    @TempDir
    Path tempDir;

    private ExecutorService executor;
    private NodeService nodeService;
    @SuppressWarnings("rawtypes")
    private org.graylog2.cluster.nodes.NodeService datanodeService;
    private RemoteInterfaceProvider remoteInterfaceProvider;
    private ObjectMapperProvider objectMapperProvider;
    private ClusterStatsService clusterStatsService;
    private VersionProbeFactory versionProbeFactory;
    private ClusterAdapter searchDbClusterAdapter;
    private DatanodeRestApiProxy datanodeProxy;
    private ProcessingLoadBuilder processingLoadBuilder;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("support-bundle-executor-%d").build());
        nodeService = new TestNodeService();
        datanodeService = mock(org.graylog2.cluster.nodes.NodeService.class);
        remoteInterfaceProvider = mock(RemoteInterfaceProvider.class);
        objectMapperProvider = new ObjectMapperProvider();
        clusterStatsService = mock(ClusterStatsService.class);
        versionProbeFactory = mock(VersionProbeFactory.class);
        searchDbClusterAdapter = mock(ClusterAdapter.class);
        datanodeProxy = mock(DatanodeRestApiProxy.class);
        processingLoadBuilder = mock(ProcessingLoadBuilder.class);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildBundleStillSucceedsAndWritesErrorsJsonWhenCollectionsFail() throws Exception {
        // First call (collectBundleData's datanode iteration): empty — no per-datanode futures.
        // Second call (inside getDatanodeInfo, run by datanodeInfoFuture): throws — triggers .exceptionally().
        when(datanodeService.allActive())
                .thenReturn(Map.of())
                .thenThrow(new RuntimeException("Simulated datanode service failure"));

        final SupportBundleService service = buildService();

        // The bundle build must succeed even though one future fails.
        assertThatNoException().isThrownBy(
                () -> service.buildBundle(mock(HttpHeaders.class), mock(Subject.class)));

        // The zip must contain errors.json with the collected error.
        try (final ZipFile zip = openBundleZip()) {
            final var errorsEntry = zip.getEntry("errors.json");
            assertThat(errorsEntry).as("errors.json entry in bundle zip").isNotNull();

            final String content = new String(
                    zip.getInputStream(errorsEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("cluster/datanode-info")
                    .contains("Simulated datanode service failure");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildBundleRecordsPerNodeFailuresFromStripCallResultInErrorsJson() throws Exception {
        // A node is registered but the remoteInterfaceProvider mock returns null,
        // so every API call to it throws an NPE inside requestOnAllNodes, producing
        // CallResult.error(...). stripCallResult should record each of those as a BundleError.
        // Stub these so fetchClusterInfos can build cluster.json without NPE-ing on
        // Map.of() null-value checks, allowing execution to reach stripCallResult.
        when(clusterStatsService.clusterStats()).thenReturn(mock(ClusterStats.class));
        when(searchDbClusterAdapter.rawClusterStats()).thenReturn(mock(JsonNode.class));

        final String failingNodeId = "failing-node-001";
        final NodeService nodeServiceWithNode = mock(NodeService.class);
        when(nodeServiceWithNode.allActive()).thenReturn(Map.of(failingNodeId, mock(Node.class)));
        when(datanodeService.allActive()).thenReturn(Map.of());

        final SupportBundleService service = new SupportBundleService(
                executor, nodeServiceWithNode, datanodeService, remoteInterfaceProvider,
                tempDir, objectMapperProvider, clusterStatsService, versionProbeFactory,
                List.of(), searchDbClusterAdapter, datanodeProxy, processingLoadBuilder);

        assertThatNoException().isThrownBy(
                () -> service.buildBundle(mock(HttpHeaders.class), mock(Subject.class)));

        try (final ZipFile zip = openBundleZip()) {
            final var errorsEntry = zip.getEntry("errors.json");
            assertThat(errorsEntry).as("errors.json entry in bundle zip").isNotNull();

            final String content = new String(
                    zip.getInputStream(errorsEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(f("node/%s/system-overview", failingNodeId))
                    .contains(f("node/%s/jvm", failingNodeId))
                    .contains(f("node/%s/process-buffer-dump", failingNodeId))
                    .contains(f("node/%s/installed-plugins", failingNodeId));
        }
    }

    @Test
    void buildBundleOmitsProcessingLoadSnapshotWhenDebugMetricsOff() throws Exception {
        when(datanodeService.allActive()).thenReturn(Map.of());
        when(processingLoadBuilder.metricsEnabled()).thenReturn(false);

        final SupportBundleService service = buildService();
        assertThatNoException().isThrownBy(
                () -> service.buildBundle(mock(HttpHeaders.class), mock(Subject.class)));

        try (final ZipFile zip = openBundleZip()) {
            assertThat(zip.getEntry("pipeline-processing-load.json"))
                    .as("processing-load snapshot must be absent when debug metrics is off")
                    .isNull();
        }
    }

    @Test
    void buildBundleOmitsProcessingLoadSnapshotWhenMetricsOnButUnavailable() throws Exception {
        when(datanodeService.allActive()).thenReturn(Map.of());
        when(processingLoadBuilder.metricsEnabled()).thenReturn(true);
        when(processingLoadBuilder.buildUnfiltered(any())).thenReturn(ProcessingLoadResponse.create(
                false, 0.0d, List.of(), List.of(), List.of()));

        final SupportBundleService service = buildService();
        assertThatNoException().isThrownBy(
                () -> service.buildBundle(mock(HttpHeaders.class), mock(Subject.class)));

        try (final ZipFile zip = openBundleZip()) {
            assertThat(zip.getEntry("pipeline-processing-load.json"))
                    .as("processing-load snapshot must be absent when no usable data is available")
                    .isNull();

            final var errorsEntry = zip.getEntry("errors.json");
            assertThat(errorsEntry).as("errors.json breadcrumb entry").isNotNull();
            final String content = new String(
                    zip.getInputStream(errorsEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("cluster/processing-load")
                    .contains("Snapshot omitted");
        }
    }

    @Test
    void buildBundleIncludesProcessingLoadSnapshotWhenDebugMetricsOn() throws Exception {
        when(datanodeService.allActive()).thenReturn(Map.of());
        when(processingLoadBuilder.metricsEnabled()).thenReturn(true);

        final ProcessingLoadResponse snapshot = ProcessingLoadResponse.create(
                true,
                170_000.0d,
                List.of(StageRuleLoad.create("rule-A", "pipe-1", 0, 29.41d, 100.0d)),
                List.of(PipelineLoad.create("pipe-1", 100.0d)),
                List.of(RuleLoad.create("rule-A", 29.41d))
        );
        when(processingLoadBuilder.buildUnfiltered(any())).thenReturn(snapshot);

        final SupportBundleService service = buildService();
        assertThatNoException().isThrownBy(
                () -> service.buildBundle(mock(HttpHeaders.class), mock(Subject.class)));

        try (final ZipFile zip = openBundleZip()) {
            final var snapshotEntry = zip.getEntry("pipeline-processing-load.json");
            assertThat(snapshotEntry).as("pipeline-processing-load.json entry").isNotNull();

            final String content = new String(
                    zip.getInputStream(snapshotEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"available\" : true")
                    .contains("rule-A")
                    .contains("pipe-1")
                    .contains("170000");
        }
    }

    private ZipFile openBundleZip() throws IOException {
        final Path bundleDir = tempDir.resolve("support-bundle");
        try (final Stream<Path> files = Files.list(bundleDir)) {
            final Path zipFile = files.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no bundle zip file was written"));
            return new ZipFile(zipFile.toFile());
        }
    }

    @SuppressWarnings("unchecked")
    private SupportBundleService buildService() {
        return new SupportBundleService(
                executor, nodeService, datanodeService, remoteInterfaceProvider,
                tempDir, objectMapperProvider, clusterStatsService, versionProbeFactory,
                List.of(), searchDbClusterAdapter, datanodeProxy, processingLoadBuilder);
    }
}
