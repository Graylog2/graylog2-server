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
package org.graylog.plugins.sidecar.collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class ConfigurationServiceTest {
    private final String FILEBEAT_CONF_ID = "5b8fe5f97ad37b17a44e2a34";

    @Mock
    private Sidecar sidecar;

    @Mock
    private NodeDetails nodeDetails;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private ConfigurationService configurationService;
    private ConfigurationVariableService configurationVariableService;
    private Configuration configuration;


    private Configuration buildTestConfig(String template) {
        return Configuration.create(FILEBEAT_CONF_ID, "collId", "filebeat", "#ffffff", template);
    }

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        when(nodeDetails.operatingSystem()).thenReturn("DummyOS");
        when(nodeDetails.ip()).thenReturn("1.2.3.4");
        when(sidecar.nodeId()).thenReturn("42");
        when(sidecar.nodeName()).thenReturn("mockymock");
        when(sidecar.nodeDetails()).thenReturn(nodeDetails);

        this.configurationVariableService = new ConfigurationVariableService(mongodb.mongoConnection(), mongoJackObjectMapperProvider);
        this.configurationService = new ConfigurationService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, configurationVariableService);
    }

    @Test
    public void testTemplateRender() throws Exception {
        final String TEMPLATE = "foo bar\n nodename: ${sidecar.nodeName}\n";
        final String TEMPLATE_RENDERED = "foo bar\n nodename: mockymock\n";
        configuration = buildTestConfig(TEMPLATE);
        this.configurationService.save(configuration);
        Configuration result = this.configurationService.renderConfigurationForCollector(sidecar, configuration);

        Configuration configWithNewline = buildTestConfig(TEMPLATE_RENDERED);
        assertEquals(configWithNewline, result);
    }

    @Test
    public void testAddMissingNewline() throws Exception {
        configuration = buildTestConfig("template\n without\n newline");
        this.configurationService.save(configuration);
        Configuration result = this.configurationService.renderConfigurationForCollector(sidecar, configuration);

        Configuration configWithNewline = buildTestConfig(configuration.template() + "\n");
        assertEquals(configWithNewline, result);
    }

    @Test
    public void testTemplateRenderWithConfigurationVariables() throws Exception {
        final String TEMPLATE = "foo bar\n myVariable: ${user.myVariable}\n";
        final String TEMPLATE_RENDERED = "foo bar\n myVariable: content of myVariable\n";
        configuration = buildTestConfig(TEMPLATE);
        this.configurationService.save(configuration);

        ConfigurationVariable myVariable = ConfigurationVariable.create("myVariable", "desc", "content of myVariable");
        this.configurationVariableService.save(myVariable);

        Configuration result = this.configurationService.renderConfigurationForCollector(sidecar, configuration);

        assertEquals(TEMPLATE_RENDERED, result.template());
    }
}
