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

import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.mongojack.DBQuery;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;

@Singleton
public class ConfigurationVariableService extends PaginatedDbService<ConfigurationVariable> {
    private static final String COLLECTION_NAME = "sidecar_configuration_variables";

    @Inject
    public ConfigurationVariableService(MongoConnection mongoConnection,
                                        MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, ConfigurationVariable.class, COLLECTION_NAME);
        db.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    public List<ConfigurationVariable> all() {
        try (final Stream<ConfigurationVariable> configurationVariableStream =
                     streamQueryWithSort(DBQuery.empty(), getSortBuilder("asc", "name"))) {
            return configurationVariableStream.collect(Collectors.toList());
        }
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
        return db.findOne(DBQuery.is("_id", id));
    }

    public ConfigurationVariable findByName(String name) {
        return db.findOne(DBQuery.is("name", name));
    }

    public boolean hasConflict(ConfigurationVariable variable) {
       final DBQuery.Query query;

       if (isNullOrEmpty(variable.id())) {
           query = DBQuery.is(ConfigurationVariable.FIELD_NAME, variable.name());
       } else {
           // updating an existing variable, don't match against itself
           query = DBQuery.and(
                           DBQuery.is(ConfigurationVariable.FIELD_NAME, variable.name()),
                           DBQuery.notEquals("_id", variable.id()
                           )
           );
       }
       return db.getCount(query) > 0;
    }

    @Override
    public ConfigurationVariable save(ConfigurationVariable configurationVariable) {
        return db.findAndModify(DBQuery.is("_id", configurationVariable.id()), new BasicDBObject(),
                new BasicDBObject(), false, configurationVariable, true, true);
    }
}
