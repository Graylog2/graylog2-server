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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.SidecarCollectorConfigurationEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SidecarCollectorConfigurationFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private SidecarCollectorConfigurationFacade facade;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final ConfigurationService configurationService = new ConfigurationService(mongodb.mongoConnection(),
                mapperProvider, new ConfigurationVariableService(mongodb.mongoConnection(), mapperProvider));

        facade = new SidecarCollectorConfigurationFacade(objectMapper, configurationService);
    }

    @Test
    @MongoDBFixtures("SidecarCollectorConfigurationFacadeTest.json")
    public void exportEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5b17e1a53f3ab8204eea1051", ModelTypes.SIDECAR_COLLECTOR_CONFIGURATION_V1);
        final EntityDescriptor collectorDescriptor = EntityDescriptor.create("5b4c920b4b900a0024af0001", ModelTypes.SIDECAR_COLLECTOR_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor, collectorDescriptor);
        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.SIDECAR_COLLECTOR_CONFIGURATION_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final SidecarCollectorConfigurationEntity configEntity = objectMapper.convertValue(entityV1.data(), SidecarCollectorConfigurationEntity.class);

        assertThat(configEntity.title()).isEqualTo(ValueReference.of("filebeat config"));
        assertThat(configEntity.collectorId()).isEqualTo(ValueReference.of(entityDescriptorIds.get(collectorDescriptor).orElse(null)));
        assertThat(configEntity.color().asString(Collections.emptyMap())).isEqualTo("#ffffff");
        assertThat(configEntity.template().asString(Collections.emptyMap())).isEqualTo("empty template");
    }
}
