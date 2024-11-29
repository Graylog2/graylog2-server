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
package org.graylog.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import one.util.streamex.StreamEx;
import org.bson.conversions.Bson;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.mongojack.DBQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class DBJobDefinitionService {
    public static final String COLLECTION_NAME = "scheduler_job_definitions";

    private final ObjectMapper objectMapper;
    private final MongoCollection<JobDefinitionDto> collection;
    private final MongoUtils<JobDefinitionDto> mongoUtils;

    @Inject
    public DBJobDefinitionService(MongoCollections mongoCollections, MongoJackObjectMapperProvider mapper) {
        collection = mongoCollections.collection(COLLECTION_NAME, JobDefinitionDto.class);
        mongoUtils = mongoCollections.utils(collection);
        objectMapper = mapper.get();
    }

    /**
     * Returns the job definition that has the given config field value.
     *
     * @param configField the config field
     * @param value       the value of the config field
     * @return the job definition with the given config field, or an empty optional
     */
    public Optional<JobDefinitionDto> getByConfigField(String configField, Object value) {
        return Optional.ofNullable(collection.find(buildConfigFieldQuery(configField, value)).first());
    }

    /**
     * Returns a stream of job definitions that have the given config field value.
     *
     * @param configField the config field
     * @param value       the value of the config field
     * @return a stream of job definitions with the given config field.
     */
    public Stream<JobDefinitionDto> streamByConfigField(String configField, Object value) {
        return MongoUtils.stream(collection.find(buildConfigFieldQuery(configField, value)));
    }

    private static Bson buildConfigFieldQuery(String configField, Object value) {
        final String field = String.format(Locale.US, "%s.%s", JobDefinitionDto.FIELD_CONFIG, configField);
        return Filters.eq(field, value);
    }

    @Deprecated
    public List<JobDefinitionDto> getByQuery(DBQuery.Query query) {
        mongoUtils.initializeLegacyMongoJackBsonObject(query);
        return getByQuery((Bson) query);
    }

    public List<JobDefinitionDto> getByQuery(Bson query) {
        return collection.find(query).into(new ArrayList<>());
    }

    /**
     * Returns all job definitions that have the given config field values, grouped by config field value.
     *
     * @param configField the config field
     * @param values      the values of the config field
     * @return the job definitions grouped by the given values
     */
    public Map<String, List<JobDefinitionDto>> getAllByConfigField(String configField, Collection<? extends Object> values) {
        final String field = String.format(Locale.US, "%s.%s", JobDefinitionDto.FIELD_CONFIG, configField);

        return StreamEx.of(collection.find(Filters.in(field, values)).iterator()).groupingBy(configFieldGroup(configField));
    }

    private Function<JobDefinitionDto, String> configFieldGroup(final String key) {
        // Since JobDefinitionDto#config() is a pluggable interface type and we don't have methods we can access,
        // convert the value to a JsonNode and access the group key with that.
        return jobDefinition -> objectMapper.convertValue(jobDefinition.config(), JsonNode.class).path(key).asText();
    }

    public JobDefinitionDto save(JobDefinitionDto jobDefinitionDto) {
        return mongoUtils.save(jobDefinitionDto);
    }

    public void delete(String id) {
        mongoUtils.deleteById(id);
    }

    public Optional<JobDefinitionDto> get(String id) {
        return mongoUtils.getById(id);
    }

    public Stream<JobDefinitionDto> streamAll() {
        return mongoUtils.stream(collection.find());
    }

    public JobDefinitionDto findOrCreate(JobDefinitionDto dto) {
        return mongoUtils.getOrCreate(dto);
    }
}
