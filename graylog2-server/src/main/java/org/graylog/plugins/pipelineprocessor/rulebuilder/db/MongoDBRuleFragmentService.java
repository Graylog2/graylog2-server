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

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

@Singleton
public class MongoDBRuleFragmentService implements RuleFragmentService {

    private static final RateLimitedLog log = getRateLimitedLog(MongoDBRuleFragmentService.class);

    public static final String COLLECTION_NAME = "rule_fragments";

    private final JacksonDBCollection<RuleFragment, ObjectId> dbCollection;

    @Inject
    public MongoDBRuleFragmentService(
            final MongoJackObjectMapperProvider objectMapperProvider,
            final MongoConnection mongoConnection
    ) {
        this(JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                RuleFragment.class,
                ObjectId.class,
                objectMapperProvider.get())
        );
    }


    public MongoDBRuleFragmentService(JacksonDBCollection<RuleFragment, ObjectId> dbCollection) {
        this.dbCollection = Objects.requireNonNull(dbCollection);

        this.dbCollection.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    @Override
    public RuleFragment save(RuleFragment ruleFragment) {
        return dbCollection.save(ruleFragment).getSavedObject();
    }

    @Override
    public void delete(String name) {
        dbCollection.remove(DBQuery.is("name", name));
    }

    @Override
    public void deleteAll() {
        dbCollection.remove(DBQuery.empty());
    }


    @Override
    public long count(String name) {
        return dbCollection.getCount(DBQuery.is("name", name));
    }

    @Override
    public Optional<RuleFragment> get(String name) {
        return Optional.ofNullable(dbCollection.findOne(DBQuery.is("name", name)));
    }

    @Override
    public Collection<RuleFragment> all() {
        try (DBCursor<RuleFragment> ruleDaos = dbCollection.find().sort(DBSort.asc("title"))) {
            return ImmutableSet.copyOf((Iterable<RuleFragment>) ruleDaos);
        } catch (MongoException e) {
            log.error("Unable to load rule fragments", e);
            return Collections.emptySet();
        }
    }


}
