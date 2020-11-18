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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.graph.Graph;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.exceptions.DivergingEntityConfigurationException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.PipelineEntity;
import org.graylog2.contentpacks.model.entities.PipelineRuleEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PipelineRuleFacadeTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private RuleService ruleService;
    private PipelineRuleFacade facade;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        ruleService = new MongoDbRuleService(
                mongodb.mongoConnection(),
                new MongoJackObjectMapperProvider(objectMapper),
                clusterEventBus);

        facade = new PipelineRuleFacade(objectMapper, ruleService);
    }

    @Test
    public void exportEntity() {
        final RuleDao pipelineRule = RuleDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")
                .build();
        final EntityDescriptor descriptor = EntityDescriptor.create("id", ModelTypes.PIPELINE_RULE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportNativeEntity(pipelineRule, entityDescriptorIds);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineRuleEntity ruleEntity = objectMapper.convertValue(entityV1.data(), PipelineRuleEntity.class);
        assertThat(ruleEntity.title()).isEqualTo(ValueReference.of("title"));
        assertThat(ruleEntity.description()).isEqualTo(ValueReference.of("description"));
        assertThat(ruleEntity.source().asString(Collections.emptyMap())).startsWith("rule \"debug\"\n");
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void exportNativeEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf25034b900a0fdb4e5338", ModelTypes.PIPELINE_RULE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Entity entity = facade.exportEntity(descriptor, entityDescriptorIds).orElseThrow(AssertionError::new);

        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);

        final EntityV1 entityV1 = (EntityV1) entity;
        final PipelineEntity pipelineEntity = objectMapper.convertValue(entityV1.data(), PipelineEntity.class);
        assertThat(pipelineEntity.title()).isEqualTo(ValueReference.of("debug"));
        assertThat(pipelineEntity.description()).isEqualTo(ValueReference.of("Debug"));
        assertThat(pipelineEntity.source().asString(Collections.emptyMap())).startsWith("rule \"debug\"\n");
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void delete() throws NotFoundException {
        final RuleDao ruleDao = ruleService.loadByName("debug");
        assertThat(ruleService.loadAll()).hasSize(2);

        facade.delete(ruleDao);
        assertThatThrownBy(() -> ruleService.loadByName("debug"))
                .isInstanceOf(NotFoundException.class);
        assertThat(ruleService.loadAll()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void findExisting() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(objectMapper.convertValue(PipelineRuleEntity.create(
                        ValueReference.of("debug"),
                        ValueReference.of("Debug"),
                        ValueReference.of("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")), JsonNode.class))
                .build();
        final NativeEntity<RuleDao> existingRule = facade.findExisting(entity, Collections.emptyMap()).orElseThrow(AssertionError::new);

        assertThat(existingRule.descriptor().id()).isEqualTo(ModelId.of("5adf25034b900a0fdb4e5338"));
        assertThat(existingRule.descriptor().type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);
        assertThat(existingRule.entity().title()).isEqualTo("debug");
        assertThat(existingRule.entity().description()).isEqualTo("Debug");
        assertThat(existingRule.entity().source()).startsWith("rule \"debug\"\n");
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void findExistingWithDifferentSource() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(objectMapper.convertValue(PipelineRuleEntity.create(
                        ValueReference.of("debug"),
                        ValueReference.of("Debug"),
                        ValueReference.of("rule \"debug\"\nwhen\n  true\nthen\n\nend")), JsonNode.class))
                .build();
        assertThatThrownBy(() -> facade.findExisting(entity, Collections.emptyMap()))
                .isInstanceOf(DivergingEntityConfigurationException.class)
                .hasMessage("Different pipeline rule sources for pipeline rule with name \"debug\"");
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void resolveEntity() {
        final Entity entity = EntityV1.builder()
                .id(ModelId.of("debug"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .data(objectMapper.convertValue(PipelineRuleEntity.create(
                        ValueReference.of("debug"),
                        ValueReference.of("Debug"),
                        ValueReference.of("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")), JsonNode.class))
                .build();
        final Graph<Entity> graph = facade.resolveForInstallation(entity, Collections.emptyMap(), Collections.emptyMap());
        assertThat(graph.nodes()).containsOnly(entity);
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void resolveEntityDescriptor() {
        final EntityDescriptor descriptor = EntityDescriptor.create("debug", ModelTypes.PIPELINE_RULE_V1);
        final Graph<EntityDescriptor> graph = facade.resolveNativeEntity(descriptor);
        assertThat(graph.nodes()).containsOnly(descriptor);
    }

    @Test
    public void createExcerpt() {
        final RuleDao pipelineRule = RuleDao.builder()
                .id("id")
                .title("title")
                .description("description")
                .source("rule \"debug\"\nwhen\n  true\nthen\n  debug($message.message);\nend")
                .build();
        final EntityExcerpt excerpt = facade.createExcerpt(pipelineRule);

        assertThat(excerpt.id()).isEqualTo(ModelId.of("id"));
        assertThat(excerpt.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);
        assertThat(excerpt.title()).isEqualTo("title");
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void listEntityExcerpts() {
        final EntityExcerpt expectedEntityExcerpt1 = EntityExcerpt.builder()
                .id(ModelId.of("5adf25034b900a0fdb4e5338"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .title("debug")
                .build();
        final EntityExcerpt expectedEntityExcerpt2 = EntityExcerpt.builder()
                .id(ModelId.of("5adf25034b900a0fdb4e5339"))
                .type(ModelTypes.PIPELINE_RULE_V1)
                .title("no-op")
                .build();

        final Set<EntityExcerpt> entityExcerpts = facade.listEntityExcerpts();
        assertThat(entityExcerpts).containsOnly(expectedEntityExcerpt1, expectedEntityExcerpt2);
    }

    @Test
    @MongoDBFixtures("PipelineRuleFacadeTest.json")
    public void collectEntity() {
        final EntityDescriptor descriptor = EntityDescriptor.create("5adf25034b900a0fdb4e5338", ModelTypes.PIPELINE_RULE_V1);
        final EntityDescriptorIds entityDescriptorIds = EntityDescriptorIds.of(descriptor);
        final Optional<Entity> collectedEntity = facade.exportEntity(descriptor, entityDescriptorIds);
        assertThat(collectedEntity)
                .isPresent()
                .containsInstanceOf(EntityV1.class);

        final EntityV1 entity = (EntityV1) collectedEntity.orElseThrow(AssertionError::new);
        assertThat(entity.id()).isEqualTo(ModelId.of(entityDescriptorIds.get(descriptor).orElse(null)));
        assertThat(entity.type()).isEqualTo(ModelTypes.PIPELINE_RULE_V1);
        final PipelineRuleEntity pipelineRuleEntity = objectMapper.convertValue(entity.data(), PipelineRuleEntity.class);
        assertThat(pipelineRuleEntity.title()).isEqualTo(ValueReference.of("debug"));
        assertThat(pipelineRuleEntity.description()).isEqualTo(ValueReference.of("Debug"));
        assertThat(pipelineRuleEntity.source().asString(Collections.emptyMap())).startsWith("rule \"debug\"\n");
    }
}
