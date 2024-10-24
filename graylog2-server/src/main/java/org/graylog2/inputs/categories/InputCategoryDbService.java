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
package org.graylog2.inputs.categories;

import com.google.common.base.Preconditions;
import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

public class InputCategoryDbService {

    private static final String COLLECTION_NAME = "input_categories";
    private final MongoCollection<InputCategoryDto> collection;
    private final MongoUtils<InputCategoryDto> mongoUtils;

    @Inject
    public InputCategoryDbService(final MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, InputCategoryDto.class);
        this.mongoUtils = mongoCollections.utils(collection);
    }

    public InputCategoryDto save(@NotNull InputCategoryDto dto) {
        return dto.toBuilder().id(insertedIdAsString(collection.insertOne(dto))).build();
    }

    public boolean update(@NotNull InputCategoryDto dto) {
        Preconditions.checkNotNull(dto.id());
        return collection.replaceOne(idEq(dto.id()), dto).wasAcknowledged();
    }

    public Optional<InputCategoryDto> get(final String id) {
        return mongoUtils.getById(id);
    }

    public List<InputCategoryDto> get() {
        return collection.find().into(new ArrayList<>());
    }

    public long delete(final String id) {
        return collection.deleteOne(idEq(id)).getDeletedCount();
    }
}
