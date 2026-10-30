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
package org.graylog2.configuration.managed;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.info.ParameterInfoCollector;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import org.graylog2.configuration.ConfigurationInfoService;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class NodeConfigurationReporterTest {
    private static final String NODE_ID = "node-1";
    private static final String PREFIX = "test.managed.";
    private static final String CONFIGURED = "configured_parameter";
    private static final String DEFAULTED = "defaulted_parameter";

    private final ManagedConfigurationService configurationService = mock(ManagedConfigurationService.class);
    private ConfigurationInfoService configurationInfoService;

    /**
     * Stands in for a real configuration bean: one parameter configured explicitly, one left at its default.
     */
    public static class TestConfiguration {
        @Documentation("A parameter which is configured explicitly")
        @Parameter(CONFIGURED)
        private String configured = "the default";

        @Documentation("A parameter which is left at its default")
        @Parameter(DEFAULTED)
        private int defaulted = 7;
    }

    @BeforeEach
    void setUp() throws RepositoryException, ValidationException {
        System.setProperty(PREFIX + CONFIGURED, "configured value");

        final var collector = new ParameterInfoCollector();
        final var jadConfig = new JadConfig(new SystemPropertiesRepository(PREFIX), new TestConfiguration());
        jadConfig.addParameterListener(collector);
        jadConfig.process();

        configurationInfoService = new ConfigurationInfoService(collector.getParameterInfos());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(PREFIX + CONFIGURED);
    }

    private NodeConfigurationReporter reporter(ConfigurationParameter... parameters) {
        final var services = mock(ManagedConfigurationServices.class);
        when(services.get(NodeType.SERVER)).thenReturn(configurationService);
        return new NodeConfigurationReporter(
                NodeType.SERVER,
                new SimpleNodeId(NODE_ID),
                new ConfigurationCatalog(Set.of(parameters)),
                configurationInfoService,
                services);
    }

    private static ConfigurationParameter parameter(String name) {
        return ConfigurationParameter.builder(NodeType.SERVER, name, name).build();
    }

    private Map<String, ReportedValue> captureReportedValues() {
        final ArgumentCaptor<Map<String, ReportedValue>> captor = ArgumentCaptor.forClass(Map.class);
        verify(configurationService).reportNodeValues(eq(NODE_ID), captor.capture());
        return captor.getValue();
    }

    @Test
    void reportsConfiguredValuesWithTheirSource() {
        reporter(parameter(CONFIGURED)).report();

        assertThat(captureReportedValues()).containsExactly(Map.entry(CONFIGURED,
                new ReportedValue("configured value", ConfigurationSource.SYSTEM_PROPERTY)));
    }

    @Test
    void reportsTheDefaultOfParametersNobodyConfigured() {
        reporter(parameter(DEFAULTED)).report();

        final var reported = captureReportedValues().get(DEFAULTED);

        // ParameterInfo#value() is null while a parameter sits at its default, the default has to be read separately
        assertThat(reported.value()).isEqualTo("7");
        assertThat(reported.source()).isEqualTo(ConfigurationSource.DEFAULT);
        assertThat(reported.isExplicit()).isFalse();
    }

    @Test
    void reportsEveryCatalogParameterOfItsNodeType() {
        reporter(parameter(CONFIGURED), parameter(DEFAULTED)).report();

        assertThat(captureReportedValues()).containsOnlyKeys(CONFIGURED, DEFAULTED);
    }

    @Test
    void skipsRuntimeParametersAndUnknownNames() {
        reporter(
                ConfigurationParameter.builder(NodeType.SERVER, "a_runtime_parameter", "Runtime")
                        .backing(ParameterBacking.RUNTIME).build(),
                parameter("not_a_real_parameter")).report();

        verify(configurationService, never()).reportNodeValues(anyString(), any());
    }

    @Test
    void adoptsExplicitlyConfiguredClusterValues() {
        reporter(ConfigurationParameter.builder(NodeType.SERVER, CONFIGURED, "Configured")
                .scope(ParameterScope.CLUSTER).build()).report();

        verify(configurationService).adoptClusterValue(eq(CONFIGURED), eq("configured value"), anyString());
    }

    @Test
    void doesNotAdoptDefaultsAsClusterValues() {
        reporter(ConfigurationParameter.builder(NodeType.SERVER, DEFAULTED, "Defaulted")
                .scope(ParameterScope.CLUSTER).build()).report();

        verify(configurationService).reportNodeValues(eq(NODE_ID), any());
        verify(configurationService, never()).adoptClusterValue(anyString(), anyString(), anyString());
    }

    @Test
    void doesNotAdoptNodeScopedValues() {
        reporter(parameter(CONFIGURED)).report();

        verify(configurationService, never()).adoptClusterValue(anyString(), anyString(), anyString());
    }
}
