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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.database.utils.ScopedEntityMongoUtils;
import org.graylog2.events.ClusterEventBus;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import static com.mongodb.client.model.Filters.regex;
import static org.graylog.plugins.pipelineprocessor.db.RuleDao.FIELD_SOURCE;
import static org.graylog.plugins.pipelineprocessor.db.RuleDao.FIELD_TITLE;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;
import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

/**
 * A RuleService backed by a MongoDB collection.
 */
public class MongoDbRuleService implements RuleService {
    private static final RateLimitedLog log = getRateLimitedLog(MongoDbRuleService.class);

    private static final String COLLECTION = "pipeline_processor_rules";

    private final MongoCollection<RuleDao> collection;
    private final ClusterEventBus clusterBus;
    private final MongoUtils<RuleDao> mongoUtils;
    private final ScopedEntityMongoUtils<RuleDao> scopedEntityMongoUtils;

    @Inject
    public MongoDbRuleService(MongoCollections mongoCollections, ClusterEventBus clusterBus, EntityScopeService entityScopeService) {
        this.collection = mongoCollections.collection(COLLECTION, RuleDao.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.clusterBus = clusterBus;
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);

        collection.createIndex(Indexes.ascending(FIELD_TITLE), new IndexOptions().unique(true));
    }

    @Override
    public RuleDao save(RuleDao rule, boolean checkMutability) {
        scopedEntityMongoUtils.ensureValidScope(rule);

        final var ruleId = rule.id();
        final RuleDao savedRule;
        if (ruleId != null) {
            if (checkMutability) {
                scopedEntityMongoUtils.ensureMutability(rule);
            }
            collection.replaceOne(MongoUtils.idEq(ruleId), rule, new ReplaceOptions().upsert(true));
            savedRule = rule;
        } else {
            final var insertedId = insertedIdAsString(collection.insertOne(rule));
            savedRule = rule.toBuilder().id(insertedId).build();
        }
        clusterBus.post(RulesChangedEvent.updatedRule(savedRule.id(), savedRule.title()));
        return savedRule;
    }

    @Override
    public RuleDao load(String id) throws NotFoundException {
        return mongoUtils.getById(id).orElseThrow(() ->
                new NotFoundException("No rule with id " + id)
        );
    }

    @Override
    public RuleDao loadByName(String name) throws NotFoundException {
        final var rule = collection.find(Filters.eq(FIELD_TITLE, name)).first();
        if (rule == null) {
            throw new NotFoundException("No rule with name " + name);
        }
        return rule;
    }

    @Override
    public Collection<RuleDao> loadBySourcePattern(String sourcePattern) {
        try {
            return collection.find(regex(FIELD_SOURCE, sourcePattern)).into(new LinkedHashSet<>());
        } catch (MongoException e) {
            log.error("Unable to load processing rules", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<RuleDao> loadAll() {
        try {
            return collection.find().sort(Sorts.ascending(FIELD_TITLE)).into(new LinkedHashSet<>());
        } catch (MongoException e) {
            log.error("Unable to load processing rules", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<RuleDao> loadAllByTitle(String regex) {
        try {
            return collection.find(Filters.regex(FIELD_TITLE, regex))
                    .sort(Sorts.ascending(FIELD_TITLE))
                    .into(new LinkedHashSet<>());
        } catch (MongoException e) {
            log.error("Unable to load processing rules", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Collection<RuleDao> loadAllByScope(String scope) {
        try {
            return collection.find(Filters.eq(FIELD_SCOPE, scope))
                    .sort(Sorts.ascending(FIELD_TITLE))
                    .into(new LinkedHashSet<>());
        } catch (MongoException e) {
            log.error("Unable to load processing rules", e);
            return Collections.emptySet();
        }
    }

    @Override
    public void delete(String id) {
        try {
            delete(load(id));
        } catch (NotFoundException e) {
            log.info("Deleting non-existant rule {}", id);
        }
    }

    @Override
    public void delete(RuleDao ruleDao) {
        if (!scopedEntityMongoUtils.deleteById(ruleDao.id(), false)) {
            log.error("Unable to delete rule {}", ruleDao.title());
        } else {
            clusterBus.post(RulesChangedEvent.deletedRule(ruleDao.id(), ruleDao.title()));
        }
    }

    @Override
    public Collection<RuleDao> loadNamed(Collection<String> ruleNames) {
        try {
            return collection.find(Filters.in(FIELD_TITLE, ruleNames)).into(new LinkedHashSet<>());
        } catch (MongoException e) {
            log.error("Unable to bulk load rules", e);
            return Collections.emptySet();
        }
    }
}
