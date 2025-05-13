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
package org.graylog.plugins.pipelineprocessor.rulebuilder.db;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.Collection;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

@Singleton
public class MongoDBRuleFragmentService implements RuleFragmentService {
    public static final String COLLECTION_NAME = "rule_fragments";

    private final MongoCollection<RuleFragment> collection;
    private final MongoUtils<RuleFragment> mongoUtils;

    @Inject
    public MongoDBRuleFragmentService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, RuleFragment.class);
        mongoUtils = mongoCollections.utils(collection);
        collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    @Override
    public RuleFragment save(RuleFragment ruleFragment) {
        return mongoUtils.save(ruleFragment);
    }

    @Override
    public void delete(String name) {
        collection.deleteOne(eq("name", name));
    }

    @Override
    public void deleteAll() {
        collection.deleteMany(Filters.empty());
    }

    @Override
    public long count(String name) {
        return collection.countDocuments(eq("name", name));
    }

    @Override
    public Optional<RuleFragment> get(String name) {
        return Optional.ofNullable(collection.find(eq("name", name)).first());
    }

    @Override
    public Collection<RuleFragment> all() {
        return ImmutableList.copyOf(collection.find().sort(Sorts.ascending("title")));
    }
}
