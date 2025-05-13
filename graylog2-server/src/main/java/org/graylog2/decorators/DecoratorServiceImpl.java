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
package org.graylog2.decorators;

import com.google.common.base.Strings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class DecoratorServiceImpl implements DecoratorService {
    private final MongoCollection<DecoratorImpl> collection;
    private final MongoUtils<DecoratorImpl> mongoUtils;

    @Inject
    public DecoratorServiceImpl(MongoCollections mongoCollections) {
        final String collectionName = DecoratorImpl.class.getAnnotation(DbEntity.class).collection();
        collection = mongoCollections.collection(collectionName, DecoratorImpl.class);
        mongoUtils = mongoCollections.utils(collection);
    }

    @Override
    public List<Decorator> findForStream(String streamId) {
        return collection.find(Filters.eq(DecoratorImpl.FIELD_STREAM, streamId)).into(new ArrayList<>());
    }

    @Override
    public List<Decorator> findForGlobal() {
        return collection.find(Filters.or(
                Filters.exists(DecoratorImpl.FIELD_STREAM, false),
                Filters.eq(DecoratorImpl.FIELD_STREAM, Optional.empty())
        )).into(new ArrayList<>());
    }

    @Override
    public Decorator findById(String decoratorId) throws NotFoundException {
        return mongoUtils.getById(decoratorId).orElseThrow(() ->
                new NotFoundException("Decorator with id " + decoratorId + " not found.")
        );
    }

    @Override
    public List<Decorator> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    @Override
    public Decorator create(String type, Map<String, Object> config, String stream, int order) {
        return DecoratorImpl.create(type, config, Optional.of(stream), order);
    }

    @Override
    public Decorator create(String type, Map<String, Object> config, int order) {
        return DecoratorImpl.create(type, config, order);
    }

    @Override
    public Decorator save(Decorator decorator) {
        checkArgument(decorator instanceof DecoratorImpl, "Argument must be an instance of DecoratorImpl, not %s", decorator.getClass());
        if (!Strings.isNullOrEmpty(decorator.id())) {
            collection.replaceOne(MongoUtils.idEq(decorator.id()), (DecoratorImpl) decorator);
            return decorator;
        }
        return mongoUtils.save((DecoratorImpl) decorator);
    }

    @Override
    public int delete(String id) {
        return mongoUtils.deleteById(id) ? 1 : 0;
    }

}
