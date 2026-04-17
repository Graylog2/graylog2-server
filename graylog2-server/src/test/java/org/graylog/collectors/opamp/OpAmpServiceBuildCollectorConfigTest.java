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
package org.graylog.collectors.opamp;

import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.CollectorReadMode;
import org.graylog.collectors.config.CollectorAttributes;
import org.graylog.collectors.config.TLSConfigurationSettings;
import org.graylog.collectors.config.exporter.OtlpExporterConfig;
import org.graylog.collectors.config.exporter.OtlpHttpExporterConfig;
import org.graylog.collectors.config.processor.ResourceProcessorConfig;
import org.graylog.collectors.db.FileSourceConfig;
import org.graylog.collectors.db.JournaldSourceConfig;
import org.graylog.collectors.db.SourceDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpAmpService#buildCollectorConfig}.
 */
class OpAmpServiceBuildCollectorConfigTest {

    private static final String FLEET_ID = "fleet-xyz";
    private static final String CA_PEM = "-----BEGIN CERTIFICATE-----\nfake\n-----END CERTIFICATE-----";
    private static final String CLUSTER_ID = "2209F727-F7E1-4123-9386-94FE3B354A07";

    private OtlpExporterConfig exporterConfig() {
        return OtlpHttpExporterConfig.builder()
                .endpoint("https://otlp.example.com:14401")
                .tls(TLSConfigurationSettings.withCACert(CLUSTER_ID, CA_PEM))
                .build();
    }

    private SourceDTO fileSource(String id) {
        return SourceDTO.builder()
                .id(id)
                .fleetId(FLEET_ID)
                .name("file-source-" + id)
                .enabled(true)
                .config(FileSourceConfig.builder()
                        .paths(List.of("/var/log/example.log"))
                        .readMode(CollectorReadMode.END)
                        .build())
                .build();
    }

    private SourceDTO disabledFileSource(String id) {
        return SourceDTO.builder()
                .id(id)
                .fleetId(FLEET_ID)
                .name("file-source-" + id)
                .enabled(false)
                .config(FileSourceConfig.builder()
                        .paths(List.of("/var/log/example.log"))
                        .readMode(CollectorReadMode.END)
                        .build())
                .build();
    }

    private SourceDTO journaldSource(String id) {
        return SourceDTO.builder()
                .id(id)
                .fleetId(FLEET_ID)
                .name("journald-source-" + id)
                .enabled(true)
                .config(JournaldSourceConfig.builder()
                        .priority("INFO")
                        .readMode(CollectorReadMode.END)
                        .build())
                .build();
    }

    @Test
    void emptySourceListProducesNoopFallback() {
        final var config = OpAmpService.buildCollectorConfig(FLEET_ID, List.of(), CollectorOSType.LINUX, exporterConfig());

        assertThat(config.receivers()).containsOnlyKeys("nop");
        assertThat(config.processors()).isEmpty();
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/nop");

        final var pipeline = config.service().pipelines().get("logs/nop");
        assertThat(pipeline.receivers()).containsExactly("nop");
        assertThat(pipeline.exporters()).containsExactly(exporterConfig().getName());
        assertThat(pipeline.processors()).isNull(); // processor key omitted on the noop pipeline
    }

    @Test
    void singleEnabledSourceProducesWiredPipeline() {
        final var source = fileSource("abc123");

        final var config = OpAmpService.buildCollectorConfig(FLEET_ID, List.of(source), CollectorOSType.LINUX, exporterConfig());

        assertThat(config.receivers()).containsOnlyKeys("file_log/abc123");
        assertThat(config.processors()).containsOnlyKeys("resource/abc123");
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/abc123");

        final var pipeline = config.service().pipelines().get("logs/abc123");
        assertThat(pipeline.receivers()).containsExactly("file_log/abc123");
        assertThat(pipeline.processors()).containsExactly("resource/abc123");
        assertThat(pipeline.exporters()).containsExactly(exporterConfig().getName());
    }

    @Test
    void processorCarriesThreeAttributesWithCorrectKeysAndValues() {
        final var source = fileSource("abc123");

        final var config = OpAmpService.buildCollectorConfig(FLEET_ID, List.of(source), CollectorOSType.LINUX, exporterConfig());

        final var processor = (ResourceProcessorConfig) config.processors().get("resource/abc123");
        assertThat(processor.attributes()).hasSize(3);
        assertThat(processor.attributes()).allSatisfy(attr ->
                assertThat(attr.action()).isEqualTo(ResourceProcessorConfig.Action.UPSERT));

        assertThat(processor.attributes()).anySatisfy(attr -> {
            assertThat(attr.key()).isEqualTo(CollectorAttributes.COLLECTOR_RECEIVER_TYPE);
            assertThat(attr.value().textValue()).isEqualTo("file_log");
        });
        assertThat(processor.attributes()).anySatisfy(attr -> {
            assertThat(attr.key()).isEqualTo(CollectorAttributes.COLLECTOR_SOURCE_ID);
            assertThat(attr.value().textValue()).isEqualTo("abc123");
        });
        assertThat(processor.attributes()).anySatisfy(attr -> {
            assertThat(attr.key()).isEqualTo(CollectorAttributes.COLLECTOR_FLEET_ID);
            assertThat(attr.value().textValue()).isEqualTo(FLEET_ID);
        });
    }

    @Test
    void disabledSourcesAreSkipped() {
        final var source = disabledFileSource("abc123");

        final var config = OpAmpService.buildCollectorConfig(FLEET_ID, List.of(source), CollectorOSType.LINUX, exporterConfig());

        // No receiver / processor / pipeline for the disabled source; noop fallback kicks in.
        assertThat(config.receivers()).containsOnlyKeys("nop");
        assertThat(config.processors()).isEmpty();
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/nop");
    }

    @Test
    void sourceWithUnsupportedOsIsSkipped() {
        // Journald receiver is LINUX-only; agent runs on WINDOWS.
        final var source = journaldSource("abc123");

        final var config = OpAmpService.buildCollectorConfig(FLEET_ID, List.of(source), CollectorOSType.WINDOWS, exporterConfig());

        assertThat(config.receivers()).containsOnlyKeys("nop");
        assertThat(config.processors()).isEmpty();
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/nop");
    }

    @Test
    void multipleSourcesProduceIndependentPipelines() {
        final var sources = List.of(fileSource("a"), fileSource("b"), journaldSource("c"));

        final var config = OpAmpService.buildCollectorConfig(FLEET_ID, sources, CollectorOSType.LINUX, exporterConfig());

        assertThat(config.receivers()).containsOnlyKeys("file_log/a", "file_log/b", "journald/c");
        assertThat(config.processors()).containsOnlyKeys("resource/a", "resource/b", "resource/c");
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/a", "logs/b", "logs/c");

        // Each pipeline wires its single receiver to its single processor — no cross-wiring.
        assertThat(config.service().pipelines().get("logs/a").receivers()).containsExactly("file_log/a");
        assertThat(config.service().pipelines().get("logs/a").processors()).containsExactly("resource/a");
        assertThat(config.service().pipelines().get("logs/b").receivers()).containsExactly("file_log/b");
        assertThat(config.service().pipelines().get("logs/b").processors()).containsExactly("resource/b");
        assertThat(config.service().pipelines().get("logs/c").receivers()).containsExactly("journald/c");
        assertThat(config.service().pipelines().get("logs/c").processors()).containsExactly("resource/c");
    }

    @Test
    void pipelineNameUsesSourceIdNotReceiverType() {
        final var config = OpAmpService.buildCollectorConfig(
                FLEET_ID, List.of(fileSource("abc123")), CollectorOSType.LINUX, exporterConfig());

        // Pipelines are keyed by source id, not by receiver type (e.g. not "logs/file_log").
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/abc123");
        assertThat(config.service().pipelines()).doesNotContainKey("logs/file_log");
        assertThat(config.service().pipelines()).doesNotContainKey("logs/file_log/abc123");
    }

    @Test
    void mixedEnabledDisabledAndOsMismatchYieldsOnlyValidSources() {
        final var enabled = fileSource("enabled");
        final var disabled = disabledFileSource("disabled");
        final var wrongOs = journaldSource("wrong-os");

        final var config = OpAmpService.buildCollectorConfig(
                FLEET_ID, List.of(enabled, disabled, wrongOs), CollectorOSType.WINDOWS, exporterConfig());

        // Only the enabled file_log source survives on Windows (journald is Linux-only; disabled is skipped).
        assertThat(config.receivers()).containsOnlyKeys("file_log/enabled");
        assertThat(config.processors()).containsOnlyKeys("resource/enabled");
        assertThat(config.service().pipelines()).containsOnlyKeys("logs/enabled");
    }

    @Test
    void allPipelinesReferenceTheSharedExporter() {
        final var exporter = exporterConfig();
        final var config = OpAmpService.buildCollectorConfig(
                FLEET_ID, List.of(fileSource("a"), fileSource("b")), CollectorOSType.LINUX, exporter);

        assertThat(config.exporters()).containsOnlyKeys(exporter.getName());
        assertThat(config.service().pipelines().values()).allSatisfy(pipeline ->
                assertThat(pipeline.exporters()).containsExactly(exporter.getName()));
    }
}
