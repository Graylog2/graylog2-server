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
package org.graylog2.streams;

import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.graylog2.database.utils.MongoUtils.idEq;

public class FavoriteFieldsServiceImpl implements FavoriteFieldsService {
    private final MongoCollection<StreamDTO> collection;
    private final StreamService streamService;

    @Inject
    public FavoriteFieldsServiceImpl(MongoCollections mongoCollections, StreamService streamService) {
        this.collection = mongoCollections.collection(StreamServiceImpl.COLLECTION_NAME, StreamDTO.class);
        this.streamService = streamService;
    }

    @Override
    public List<String> get(String streamId) throws NotFoundException {
        final var fields = streamService.load(streamId).getFavoriteFields();
        return Optional.ofNullable(fields).orElse(List.of());
    }

    @Override
    public boolean set(String streamId, List<String> fields) {
        return collection.updateOne(idEq(streamId),
                Updates.set(StreamDTO.FIELD_FAVORITE_FIELDS, fields)
        ).wasAcknowledged();
    }

    @Override
    public boolean add(String streamId, String field) {
        return collection.updateOne(idEq(streamId),
                Updates.addToSet(StreamDTO.FIELD_FAVORITE_FIELDS, field)
        ).wasAcknowledged();
    }

    @Override
    public boolean remove(String streamId, String field) {
        return collection.updateOne(idEq(streamId),
                Updates.pull(StreamDTO.FIELD_FAVORITE_FIELDS, field)
        ).wasAcknowledged();
    }
}
