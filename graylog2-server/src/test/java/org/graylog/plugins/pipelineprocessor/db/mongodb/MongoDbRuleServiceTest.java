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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import org.bson.types.ObjectId;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.SystemPipelineRuleScope;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.events.ClusterEventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class MongoDbRuleServiceTest {
    @Mock
    ClusterEventBus clusterEventBus;

    EntityScopeService entityScopeService;
    MongoDbRuleService ruleService;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        entityScopeService = new EntityScopeService(Set.of(new DefaultEntityScope(), new SystemPipelineRuleScope()));
        ruleService = new MongoDbRuleService(mongoCollections, clusterEventBus, entityScopeService);
    }

    @Test
    void create() throws Exception {
        final var rule = dummyRule();
        final var savedRule = ruleService.save(rule);
        assertThat(ruleService.load(savedRule.id())).isEqualTo(rule.toBuilder().id(savedRule.id()).build());
        verify(clusterEventBus).post(RulesChangedEvent.updatedRuleId(savedRule.id()));
    }

    @Test
    void createSystemRuleSucceeds() throws Exception {
        final var rule = systemRule();
        final var savedRule = ruleService.save(rule);
        assertThat(ruleService.load(savedRule.id())).isEqualTo(rule.toBuilder().id(savedRule.id()).build());
        verify(clusterEventBus).post(RulesChangedEvent.updatedRuleId(savedRule.id()));
    }

    @Test
    void update() throws Exception {
        final var rule = dummyRule().toBuilder().id(new ObjectId().toHexString()).build();
        final var savedRule = ruleService.save(rule);
        assertThat(ruleService.load(savedRule.id())).isEqualTo(rule);
        verify(clusterEventBus).post(RulesChangedEvent.updatedRuleId(savedRule.id()));
    }

    @Test
    void updateSystemRuleThrows() {
        final var rule = systemRule().toBuilder().id(new ObjectId().toHexString()).build();
        assertThatThrownBy(() -> ruleService.save(rule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Immutable entity cannot be modified");
        verify(clusterEventBus, times(0)).post(any());
    }

    @Test
    void loadByName() throws Exception {
        final var rule = dummyRule();
        final var savedRule = ruleService.save(rule);
        assertThat(ruleService.loadByName(rule.title())).isEqualTo(savedRule);
    }

    @Test
    void loadAll() {
        final var rule1 = ruleService.save(dummyRule().toBuilder().title("title 3").build());
        final var rule2 = ruleService.save(dummyRule().toBuilder().title("title 2").build());
        final var rule3 = ruleService.save(dummyRule().toBuilder().title("title 1").build());

        assertThat(ruleService.loadAll()).containsExactly(rule3, rule2, rule1);
    }

    @Test
    void delete() {
        final var rule = ruleService.save(dummyRule().toBuilder().title("title 1").build());
        assertThat(ruleService.loadAll()).hasSize(1);
        ruleService.delete(rule.id());
        assertThat(ruleService.loadAll()).hasSize(0);
        verify(clusterEventBus).post(RulesChangedEvent.deletedRuleId(rule.id()));
    }

    @Test
    void deleteSystemRuleSucceeds() {
        final var rule = ruleService.save(systemRule().toBuilder().title("title 2").build());
        assertThat(ruleService.loadAll()).hasSize(1);
        ruleService.delete(rule.id());
        assertThat(ruleService.loadAll()).hasSize(0);
        verify(clusterEventBus).post(RulesChangedEvent.deletedRuleId(rule.id()));
    }

    @Test
    void loadNamed() {
        final var rule1 = ruleService.save(dummyRule().toBuilder().title("title 1").build());
        final var ignored = ruleService.save(dummyRule().toBuilder().title("title 2").build());
        final var rule3 = ruleService.save(dummyRule().toBuilder().title("title 3").build());
        assertThat(ruleService.loadNamed(Set.of("title 1", "title 3", "title 4"))).containsExactlyInAnyOrder(
                rule1, rule3
        );
    }

    private static RuleDao dummyRule() {
        return RuleDao.builder().title("a title").source("a source").build();
    }

    private static RuleDao systemRule() {
        return RuleDao.builder().scope(SystemPipelineRuleScope.NAME).title("a sysytem rule").source("a source").build();
    }

}
