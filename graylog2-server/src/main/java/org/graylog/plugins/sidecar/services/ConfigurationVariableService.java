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
package org.graylog.plugins.sidecar.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mongodb.client.model.Filters.eq;
import static org.graylog.plugins.sidecar.rest.models.ConfigurationVariable.FIELD_NAME;

@Singleton
public class ConfigurationVariableService {
    private static final String COLLECTION_NAME = "sidecar_configuration_variables";

    private final MongoCollection<ConfigurationVariable> collection;
    private final MongoUtils<ConfigurationVariable> mongoUtils;

    @Inject
    public ConfigurationVariableService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, ConfigurationVariable.class);
        mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(Indexes.ascending(FIELD_NAME), new IndexOptions().unique(true));
    }

    public List<ConfigurationVariable> all() {
        return collection.find().sort(Sorts.ascending(FIELD_NAME)).into(new ArrayList<>());
    }

    public ConfigurationVariable fromRequest(ConfigurationVariable request) {
        return ConfigurationVariable.create(
                request.name(),
                request.description(),
                request.content());
    }

    public ConfigurationVariable fromRequest(String id, ConfigurationVariable request) {
        return ConfigurationVariable.create(
                id,
                request.name(),
                request.description(),
                request.content());
    }

    public ConfigurationVariable find(String id) {
        return mongoUtils.getById(id).orElse(null);
    }

    public ConfigurationVariable findByName(String name) {
        return collection.find(eq(FIELD_NAME, name)).first();
    }

    public boolean hasConflict(ConfigurationVariable variable) {
        final Bson filter;

        if (isNullOrEmpty(variable.id())) {
            filter = eq(FIELD_NAME, variable.name());
        } else {
            // updating an existing variable, don't match against itself
            filter = Filters.and(
                    eq(FIELD_NAME, variable.name()),
                    Filters.ne("_id", new ObjectId(variable.id()))
            );
        }

        return collection.countDocuments(filter) > 0;
    }

    public ConfigurationVariable save(ConfigurationVariable configurationVariable) {
        return collection.findOneAndReplace(MongoUtils.idEq(Objects.requireNonNull(configurationVariable.id())),
                configurationVariable,
                new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
    }

    public int delete(String id) {
        return mongoUtils.deleteById(id) ? 1 : 0;
    }
}
