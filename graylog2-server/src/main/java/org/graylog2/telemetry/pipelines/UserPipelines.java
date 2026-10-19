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
package org.graylog2.telemetry.pipelines;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.DefaultEntityScope;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.in;
import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;

/**
 * Finds user-defined pipelines and rules, ie those with DEFAULT scope.
 */
@Singleton
public class UserPipelines {
    private static final String FIELD_ID = "_id";

    // A missing or null scope deserializes as DEFAULT, so it counts as user-defined too.
    private static final Bson USER_SCOPE = in(FIELD_SCOPE, DefaultEntityScope.NAME, null);

    private final MongoCollection<Document> pipelines;
    private final MongoCollection<Document> rules;

    @Inject
    public UserPipelines(MongoCollections mongoCollections) {
        this.pipelines = mongoCollections.nonEntityCollection(MongoDbPipelineService.COLLECTION, Document.class);
        this.rules = mongoCollections.nonEntityCollection(MongoDbRuleService.COLLECTION, Document.class);
    }

    public Set<String> pipelineIds() {
        // Only the ID is needed, so skip loading the pipeline source.
        return pipelines.find(USER_SCOPE)
                .projection(Projections.include(FIELD_ID))
                .map(doc -> doc.get(FIELD_ID).toString())
                .into(new HashSet<>());
    }

    public long ruleCount() {
        return rules.countDocuments(USER_SCOPE);
    }
}
