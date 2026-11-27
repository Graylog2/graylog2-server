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
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.info.ParameterInfoCollector;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.google.common.eventbus.EventBus;
import org.graylog2.configuration.ConfigurationInfoService;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the applier against a real JadConfig instance, so the whole path from a stored value to the field of a
 * configuration bean is covered. Only the database is replaced.
 */
@SuppressWarnings("unchecked")
class ManagedConfigurationApplierTest {
    private static final String NODE_ID = "node-1";
    private static final String MANAGED = "managed_parameter";
    private static final String MANAGED_NUMBER = "managed_number";
    private static final String UNREGISTERED = "unregistered_parameter";
    private static final String RUNTIME = "a.runtime.setting";
    /**
     * A value of {@link #MANAGED_NUMBER} which converts and passes the parameter validators, but is refused by a
     * validator method of the bean, like a check across several parameters would.
     */
    private static final int REFUSED_BY_BEAN = 13;

    /**
     * Stands in for the configuration bean of a node.
     */
    public static class TestConfiguration {
        @Documentation("A parameter which is registered in the catalog")
        @Parameter(MANAGED)
        private String managed = "the default";

        @Documentation("A number which is registered in the catalog")
        @Parameter(MANAGED_NUMBER)
        private int managedNumber = 7;

        @Documentation("A parameter which isn't registered in the catalog")
        @Parameter(UNREGISTERED)
        private String unregistered = "the default";

        @ValidatorMethod
        @SuppressWarnings("unused")
        public void validate() throws ValidationException {
            if (managedNumber == REFUSED_BY_BEAN) {
                throw new ValidationException(MANAGED_NUMBER + " must not be " + REFUSED_BY_BEAN);
            }
        }
    }

    private final TestConfiguration configuration = new TestConfiguration();
    private final ManagedConfigurationService configurationService = mock(ManagedConfigurationService.class);
    private final Map<String, String> storedValues = new HashMap<>();
    private final EventBus eventBus = new EventBus();
    private ConfigurationInfoService configurationInfoService;
    private ConfigurationReprocessor reprocessor;
    private ConfigurationCatalog catalog;
    private ManagedConfigurationServices configurationServices;
    private NodeConfigurationReporter reporter;

    @BeforeEach
    void setUp() throws Exception {
        final var collector = new ParameterInfoCollector();
        final var jadConfig = new JadConfig(new InMemoryRepository(Map.of(MANAGED, "from the file")), configuration);
        jadConfig.addParameterListener(collector);
        jadConfig.process();

        configurationInfoService = new ConfigurationInfoService(collector.getParameterInfos());
        reprocessor = new ConfigurationReprocessor(jadConfig, collector, configurationInfoService);
        catalog = new ConfigurationCatalog(Set.of(
                ConfigurationParameter.builder(NodeType.SERVER, MANAGED, "Managed").build(),
                ConfigurationParameter.builder(NodeType.SERVER, MANAGED_NUMBER, "Managed number")
                        .type(ParameterType.INTEGER).build(),
                ConfigurationParameter.builder(NodeType.SERVER, RUNTIME, "Runtime")
                        .backing(ParameterBacking.RUNTIME).build()));

        when(configurationService.nodeType()).thenReturn(NodeType.SERVER);
        when(configurationService.storedValues(NODE_ID)).thenAnswer(invocation -> Map.copyOf(storedValues));
        configurationServices = mock(ManagedConfigurationServices.class);
        when(configurationServices.get(NodeType.SERVER)).thenReturn(configurationService);
        reporter = new NodeConfigurationReporter(NodeType.SERVER, new SimpleNodeId(NODE_ID), catalog,
                configurationInfoService, configurationServices);
    }

    private ManagedConfigurationApplier applier(String... ignoredParameters) {
        return new ManagedConfigurationApplier(NodeType.SERVER, new SimpleNodeId(NODE_ID), catalog,
                configurationServices, reprocessor, reporter, eventBus, Set.of(ignoredParameters));
    }

    private Map<String, ReportedValue> lastReportedValues() {
        final ArgumentCaptor<Map<String, ReportedValue>> captor = ArgumentCaptor.forClass(Map.class);
        verify(configurationService, atLeastOnce()).reportNodeValues(eq(NODE_ID), captor.capture());
        return captor.getValue();
    }

    @Test
    void storedValueWinsOverTheConfigurationOfTheNode() {
        storedValues.put(MANAGED, "from the database");

        applier().refresh();

        assertThat(configuration.managed).isEqualTo("from the database");
        assertThat(ConfigurationSource.of(configurationInfoService.get(MANAGED).orElseThrow()))
                .isEqualTo(ConfigurationSource.DATABASE);
        assertThat(lastReportedValues().get(MANAGED))
                .isEqualTo(new ReportedValue("from the database", ConfigurationSource.DATABASE));
    }

    @Test
    void removedValueFallsBackToTheConfigurationOfTheNode() {
        final var applier = applier();
        storedValues.put(MANAGED, "from the database");
        applier.refresh();

        storedValues.clear();
        applier.refresh();

        assertThat(configuration.managed).isEqualTo("from the file");
        assertThat(lastReportedValues().get(MANAGED).value()).isEqualTo("from the file");
        assertThat(lastReportedValues().get(MANAGED).source()).isNotEqualTo(ConfigurationSource.DATABASE);
    }

    @Test
    void removedValueFallsBackToTheDefaultWhenTheNodeDoesNotConfigureTheParameter() {
        final var applier = applier();
        storedValues.put(MANAGED_NUMBER, "12");
        applier.refresh();
        assertThat(configuration.managedNumber).isEqualTo(12);

        storedValues.clear();
        applier.refresh();

        // JadConfig leaves a field alone when no repository has a value for it, so the stored value sticks
        assertThat(configuration.managedNumber).isEqualTo(7);
        assertThat(lastReportedValues().get(MANAGED_NUMBER).value()).isEqualTo("7");
    }

    @Test
    void fallsBackToTheDefaultWhenAStoredValueIsRefusedByAValidatorMethod() {
        storedValues.put(MANAGED_NUMBER, String.valueOf(REFUSED_BY_BEAN));

        applier().refresh();

        // The field has been set before the validator method refused it, and processing again without the stored
        // values doesn't reset it, so the node keeps running with the refused value
        assertThat(configuration.managedNumber).isEqualTo(7);
    }

    @Test
    void ignoresStoredValuesOfParametersWhichAreNotRegistered() {
        storedValues.put(UNREGISTERED, "from the database");

        applier().refresh();

        assertThat(configuration.unregistered).isEqualTo("the default");
    }

    @Test
    void fallsBackToTheConfigurationOfTheNodeWhenAStoredValueCannotBeProcessed() {
        storedValues.put(MANAGED, "from the database");
        storedValues.put(MANAGED_NUMBER, "not a number");

        applier().refresh();

        assertThat(configuration.managedNumber).isEqualTo(7);
        assertThat(configuration.managed).isEqualTo("from the file");
    }

    @Test
    void recoversOnceTheStoredValueIsCorrected() {
        final var applier = applier();
        storedValues.put(MANAGED_NUMBER, "not a number");
        applier.refresh();

        storedValues.put(MANAGED_NUMBER, "12");
        applier.refresh();

        assertThat(configuration.managedNumber).isEqualTo(12);
    }

    @Test
    void picksUpChangesAnnouncedForThisNodeOrForAllNodesOfItsType() {
        applier();

        storedValues.put(MANAGED, "for this node");
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.SERVER, NODE_ID, Set.of(MANAGED)));
        assertThat(configuration.managed).isEqualTo("for this node");

        storedValues.put(MANAGED, "for all nodes");
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.SERVER, null, Set.of(MANAGED)));
        assertThat(configuration.managed).isEqualTo("for all nodes");
    }

    @Test
    void ignoresChangesAnnouncedForOtherNodes() {
        applier();
        storedValues.put(MANAGED, "from the database");

        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.SERVER, "another-node", Set.of(MANAGED)));
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.DATANODE, null, Set.of(MANAGED)));

        assertThat(configuration.managed).isEqualTo("from the file");
    }

    @Test
    void handsOverOnlyRuntimeParameters() {
        storedValues.put(MANAGED, "from the database");
        storedValues.put(RUNTIME, "85%");
        storedValues.put("not.registered", "anything");

        assertThat(applier().runtimeValues()).containsExactly(Map.entry(RUNTIME, "85%"));
    }

    @Test
    void recordsWhenTheProcessStartedWithTheLoadedValues() {
        final var applier = applier();
        storedValues.put(MANAGED, "from the database");

        applier.prepareProcessStart();

        assertThat(configuration.managed).isEqualTo("from the database");
        verify(configurationService).markConfigurationLoaded(NODE_ID);
        verify(configurationService, never()).markConfigurationApplied(NODE_ID);

        applier.processAvailable();

        verify(configurationService).markConfigurationApplied(NODE_ID);
    }

    @Test
    void ignoresTheStoredValuesOfNamedParametersOnly() {
        storedValues.put(MANAGED, "from the database");
        storedValues.put(MANAGED_NUMBER, "12");
        storedValues.put(RUNTIME, "85%");

        final var applier = applier(" " + MANAGED + " ", RUNTIME);
        applier.refresh();

        assertThat(configuration.managed).isEqualTo("from the file");
        assertThat(configuration.managedNumber).isEqualTo(12);
        assertThat(applier.runtimeValues()).isEmpty();
    }

    @Test
    void leavesTheConfigurationAloneWhenAllStoredValuesAreIgnored() {
        final var applier = applier(ManagedConfigurationApplier.IGNORE_ALL);
        storedValues.put(MANAGED, "from the database");
        storedValues.put(RUNTIME, "85%");

        applier.refresh();
        eventBus.post(new ManagedConfigurationChangedEvent(NodeType.SERVER, NODE_ID, Set.of(MANAGED)));
        applier.prepareProcessStart();

        assertThat(configuration.managed).isEqualTo("from the file");
        assertThat(applier.runtimeValues()).isEmpty();
        // A restart still has to be visible to whoever waits for it
        verify(configurationService).markConfigurationLoaded(NODE_ID);
    }
}
