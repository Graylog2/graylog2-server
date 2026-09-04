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
import org.graylog.plugins.sidecar.template.RenderTemplateException;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ConfigurationServiceTest {
    private final String FILEBEAT_CONF_ID = "5b8fe5f97ad37b17a44e2a34";

    @Mock
    private Sidecar sidecar;

    @Mock
    private NodeDetails nodeDetails;

    private ConfigurationService configurationService;
    private ConfigurationVariableService configurationVariableService;
    private Configuration configuration;


    private Configuration buildTestConfig(String template) {
        return Configuration.create(FILEBEAT_CONF_ID, "collId", "filebeat", "#ffffff", template, Set.of());
    }

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        when(nodeDetails.operatingSystem()).thenReturn("DummyOS");
        when(nodeDetails.ip()).thenReturn("1.2.3.4");
        when(sidecar.nodeId()).thenReturn("42");
        when(sidecar.nodeName()).thenReturn("mockymock");
        when(sidecar.nodeDetails()).thenReturn(nodeDetails);

        this.configurationVariableService = new ConfigurationVariableService(mongoCollections);
        this.configurationService = new ConfigurationService(
                mongoCollections,
                configurationVariableService,
                new SecureFreemarkerConfigProvider());
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
    public void testTemplateRenderUsingForbiddenFeatures() throws Exception {
        final String TEMPLATE = "<#assign ex=\"freemarker.template.utility.Execute\"?new()> ${ex(\"date\")}\n nodename: ${sidecar.nodeName}\n";

        assertThrows(RenderTemplateException.class, () -> {
            configuration = buildTestConfig(TEMPLATE);
            this.configurationService.save(configuration);
            this.configurationService.renderConfigurationForCollector(sidecar, configuration);
        }, "Template should not allow insecure features");
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
