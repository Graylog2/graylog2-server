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
package org.graylog.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.TestEventProcessorParameters;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DBJobDefinitionServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private DBJobDefinitionService service;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(new NamedType(TestEventProcessorConfig.class, TestEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TestEventProcessorParameters.class, TestEventProcessorParameters.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(EventProcessorExecutionJob.Config.class, EventProcessorExecutionJob.TYPE_NAME));

        this.service = new DBJobDefinitionService(mongodb.mongoConnection(), new MongoJackObjectMapperProvider(objectMapper));
    }

    @Test
    @MongoDBFixtures("job-definitions-with-triggers.json")
    public void getAllByConfigField() {
        final String eventDefinitionId = "54e3deadbeefdeadbeef0000";
        final Map<String, List<JobDefinitionDto>> result = service.getAllByConfigField("event_definition_id", Collections.singleton(eventDefinitionId));

        assertThat(result).isNotEmpty();
        assertThat(result.get(eventDefinitionId)).hasSize(2);
        assertThat(result.get(eventDefinitionId).get(0)).satisfies(jobDefinition -> {
            assertThat(jobDefinition.id()).isEqualTo("54e3deadbeefdeadbeef0000");
            assertThat(jobDefinition.title()).isEqualTo("Test 1");
        });
        assertThat(result.get(eventDefinitionId).get(1)).satisfies(jobDefinition -> {
            assertThat(jobDefinition.id()).isEqualTo("54e3deadbeefdeadbeef0001");
            assertThat(jobDefinition.title()).isEqualTo("Test 2");
        });
    }

    @Test
    @MongoDBFixtures("job-definitions-with-triggers.json")
    public void getAllByConfigFieldWithEmptyValues() {
        final Map<String, List<JobDefinitionDto>> result = service.getAllByConfigField("event_definition_id", Collections.singleton("unknown-id"));
        assertThat(result).isEmpty();
    }
}
