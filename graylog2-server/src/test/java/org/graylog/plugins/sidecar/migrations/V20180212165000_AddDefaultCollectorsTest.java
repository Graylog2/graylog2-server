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
package org.graylog.plugins.sidecar.migrations;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.graylog.plugins.sidecar.common.SidecarPluginConfiguration;
import org.graylog.plugins.sidecar.migrations.V20180212165000_AddDefaultCollectors.MigrationState;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class V20180212165000_AddDefaultCollectorsTest {

    // Deep stubs make removeConfigPath()'s mongo chain a no-op without explicit stubbing.
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MongoConnection mongoConnection;
    @Mock
    private HttpConfiguration httpConfiguration;
    @Mock
    private CollectorService collectorService;
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private ConfigurationVariableService configurationVariableService;
    @Mock
    private ClusterConfigService clusterConfigService;
    @Captor
    private ArgumentCaptor<Collector> collectorCaptor;
    @Captor
    private ArgumentCaptor<ConfigurationVariable> variableCaptor;

    // Custom package layout shared by the configured-path tests.
    private static final Map<String, String> CUSTOM_PATHS = Map.of(
            "sidecar_collector_binary_dir", "/usr/lib/custom-sidecar",
            "sidecar_spool_dir", "/var/lib/custom-sidecar",
            "sidecar_windows_install_dir", "C:\\Program Files\\custom\\sidecar"
    );

    @BeforeEach
    void setUp() {
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://graylog.example.com:9000/"));
        when(clusterConfigService.getOrDefault(eq(MigrationState.class), any())).thenReturn(MigrationState.createEmpty());
        // save() echoes the entity back with an id so the migration completes.
        when(collectorService.save(any())).thenAnswer(
                inv -> ((Collector) inv.getArgument(0)).toBuilder().id("000000000000000000000001").build()
        );
        when(configurationService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configurationVariableService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void seedsDefaultPathsMatchingTheGraylogSidecarPackages() {
        final var collectors = migrate(new SidecarPluginConfiguration());

        assertThat(exec(collectors, "filebeat|linux")).isEqualTo("/usr/lib/graylog-sidecar/filebeat");
        assertThat(exec(collectors, "auditbeat|linux")).isEqualTo("/usr/lib/graylog-sidecar/auditbeat");
        assertThat(exec(collectors, "filebeat|windows")).isEqualTo("C:\\Program Files\\Graylog\\sidecar\\filebeat.exe");
        assertThat(exec(collectors, "winlogbeat|windows")).isEqualTo("C:\\Program Files\\Graylog\\sidecar\\winlogbeat.exe");

        // spoolDir fallbacks must stay byte-identical, or the migration's CRC check breaks on upgrades.
        assertThat(template(collectors, "filebeat|linux")).contains("/var/lib/graylog-sidecar/collectors/filebeat");
        assertThat(template(collectors, "filebeat|windows")).contains("C:\\\\Program Files\\\\Graylog\\\\sidecar\\\\cache\\\\filebeat");

        // Default install keeps the graylog_host reference (backwards compatible).
        assertThat(template(collectors, "filebeat|linux")).contains("${user.graylog_host}");
    }

    @Test
    void appliesConfiguredPathsToEveryCollector() throws Exception {
        final var collectors = migrate(configWith(CUSTOM_PATHS));

        assertThat(exec(collectors, "filebeat|linux")).isEqualTo("/usr/lib/custom-sidecar/filebeat");
        assertThat(exec(collectors, "winlogbeat|windows")).isEqualTo("C:\\Program Files\\custom\\sidecar\\winlogbeat.exe");
        assertThat(template(collectors, "filebeat|linux")).contains("/var/lib/custom-sidecar/collectors/filebeat");
        assertThat(template(collectors, "filebeat|windows")).contains("C:\\\\Program Files\\\\custom\\\\sidecar\\\\cache\\\\filebeat");

        assertThat(collectors.values()).allSatisfy(c ->
                assertThat(c.executablePath() + " " + c.defaultTemplate()).doesNotContain("graylog-sidecar"));
        assertThat(template(collectors, "auditbeat|linux")).contains("/etc/graylog/server");
    }

    @Test
    void usesConfiguredHostVariable() throws Exception {
        final var collectors = migrate(configWith(Map.of("sidecar_host_variable", "custom_host")));

        assertThat(collectors.values()).allSatisfy(c ->
                assertThat(c.defaultTemplate())
                        .contains("${user.custom_host}")
                        .doesNotContain("${user.graylog_host}")
        );

        verify(configurationVariableService).save(variableCaptor.capture());
        assertThat(variableCaptor.getValue().name()).isEqualTo("custom_host");
        assertThat(variableCaptor.getValue().description()).doesNotContain("Graylog");
    }

    @Test
    void usesConfiguredServerConfigDirInAuditbeatFileIntegrity() throws Exception {
        final var collectors = migrate(configWith(Map.of("sidecar_server_config_dir", "/etc/acme/server")));

        assertThat(template(collectors, "auditbeat|linux"))
                .contains("/etc/acme/server")
                .doesNotContain("/etc/graylog/server");
    }

    private Map<String, Collector> migrate(SidecarPluginConfiguration config) {
        new V20180212165000_AddDefaultCollectors(httpConfiguration, collectorService, configurationVariableService,
                configurationService, mongoConnection, clusterConfigService, config).upgrade();

        verify(collectorService, atLeastOnce()).save(collectorCaptor.capture());
        return collectorCaptor.getAllValues().stream()
                .collect(Collectors.toMap(c -> c.name() + "|" + c.nodeOperatingSystem(), c -> c));
    }

    private static SidecarPluginConfiguration configWith(Map<String, String> properties) throws Exception {
        final var config = new SidecarPluginConfiguration();
        new JadConfig(new InMemoryRepository(properties), config).process();
        return config;
    }

    private static String exec(Map<String, Collector> collectors, String key) {
        return collectors.get(key).executablePath();
    }

    private static String template(Map<String, Collector> collectors, String key) {
        return collectors.get(key).defaultTemplate();
    }
}
