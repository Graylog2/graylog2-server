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
package org.graylog2.indexer.fieldtypes.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.NonDeletableSystemScope;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.indexset.IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
public class FieldTypesMappingServiceIT {
    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private final String indexSetId = "57f3d721b43c2d59cb750001";
    private final IndexSetConfig indexSetConfig = IndexSetConfig.create(
            indexSetId,
            "Test custom field mappings",
            "A test index-set.",
            true, true,
            "mappingTest_",
            4,
            1,
            MessageCountRotationStrategy.class.getCanonicalName(),
            MessageCountRotationStrategyConfig.create(1000),
            NoopRetentionStrategy.class.getCanonicalName(),
            NoopRetentionStrategyConfig.create(10),
            ZonedDateTime.now(ZoneOffset.UTC),
            "standard",
            "graylog3-template",
            DEFAULT_INDEX_TEMPLATE_TYPE,
            1,
            false
    );

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private MongoIndexSetService indexSetService;
    private FieldTypeMappingsService fieldTypeMappingsService;

    @Mock
    private StreamService streamService;
    @Mock
    private MongoIndexSet.Factory mongoIndexSetFactory;
    @Mock
    private IndexFieldTypeProfileService indexFieldTypeProfileService;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) {
        final ClusterEventBus clusterEventBus = new ClusterEventBus();
        final ClusterConfigService clusterConfigService = new ClusterConfigServiceImpl(objectMapperProvider, mongoCollections.mongoConnection(),
                nodeId, new RestrictedChainingClassLoader(
                new ChainingClassLoader(getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                clusterEventBus);
        final EntityScopeService entityScopeService = new EntityScopeService(Set.of(new DefaultEntityScope(), new NonDeletableSystemScope()));
        this.indexSetService = new MongoIndexSetService(mongoCollections, streamService, clusterConfigService, clusterEventBus, entityScopeService);
        this.fieldTypeMappingsService = new FieldTypeMappingsService(indexSetService, mongoIndexSetFactory, indexFieldTypeProfileService);
        this.indexSetService.save(indexSetConfig);
    }

    @Test
    void testChangeFieldType() {
        final var indexSetIds = Set.of(this.indexSetId);
        FieldTypeMappingsService spy = spy(fieldTypeMappingsService);
        doThrow(new RuntimeException("Crash in OpenSearch")).when(spy).cycleIndexSet(any());

        final var customFieldMapping1 = new CustomFieldMapping("dummy", "long");
        spy.changeFieldType(customFieldMapping1, indexSetIds, false);

        var ids = indexSetService.get(indexSetId);
        assertThat(ids).isPresent();
        assertThat(ids.get().customFieldMappings().stream().filter(cm -> cm.fieldName().equals("dummy") && cm.type().equals("long")).findFirst()).isPresent();
        assertThat(ids.get().customFieldMappings().stream().filter(cm -> cm.fieldName().equals("dummy") && cm.type().equals("double")).findFirst()).isEmpty();

        final var customFieldMapping2 = new CustomFieldMapping("dummy", "double");
        // this should fail, no changes are written
        assertThrows(RuntimeException.class, () -> spy.changeFieldType(customFieldMapping2, indexSetIds, true));

        ids = indexSetService.get(indexSetId);
        assertThat(ids).isPresent();
        assertThat(ids.get().customFieldMappings().stream().filter(cm -> cm.fieldName().equals("dummy") && cm.type().equals("long")).findFirst()).isPresent();
        assertThat(ids.get().customFieldMappings().stream().filter(cm -> cm.fieldName().equals("dummy") && cm.type().equals("double")).findFirst()).isEmpty();
    }
}
