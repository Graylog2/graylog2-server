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
package org.graylog.events.notifications;

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.database.GraylogMongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.graylog.events.notifications.NotificationDto.FIELD_ID;
import static org.graylog2.database.MongoUtils.insertedIdAsString;
import static org.graylog2.database.MongoUtils.stream;

public class DBNotificationService {
    private static final String NOTIFICATION_COLLECTION_NAME = "event_notifications";

    private final EntityOwnershipService entityOwnerShipService;
    private final GraylogMongoCollection<NotificationDto> collection;

    @Inject
    public DBNotificationService(MongoCollections mongoCollections,
                                 EntityOwnershipService entityOwnerShipService) {
        this.collection = mongoCollections.get(NOTIFICATION_COLLECTION_NAME, NotificationDto.class);
        this.entityOwnerShipService = entityOwnerShipService;
    }

    public PaginatedList<NotificationDto> searchPaginated(SearchQuery query, Predicate<NotificationDto> filter,
                                                          String sortByField, String sortOrder, int page, int perPage) {
        return collection.findPaginated()
                .filter(query.toBson())
                .sort(sortByField, sortOrder)
                .perPage(perPage)
                .postProcessedPage(page, filter);
    }

    public NotificationDto saveWithOwnership(NotificationDto notificationDto, User user) {
        final NotificationDto dto = save(notificationDto);
        entityOwnerShipService.registerNewEventNotification(dto.id(), user);
        return dto;
    }

    public NotificationDto save(NotificationDto notificationDto) {
        if (notificationDto.id() != null) {
            collection.replaceOne(Filters.eq(FIELD_ID, new ObjectId(notificationDto.id())), notificationDto);
            return notificationDto;
        } else {
            var id = insertedIdAsString(collection.insertOne(notificationDto));
            return notificationDto.toBuilder().id(id).build();
        }
    }

    public int delete(String id) {
        entityOwnerShipService.unregisterEventNotification(id);
        return (int) collection.deleteOne(Filters.eq(FIELD_ID, new ObjectId(id))).getDeletedCount();
    }

    public Optional<NotificationDto> get(String id) {
        return collection.getById(id);
    }

    public Stream<NotificationDto> streamAll() {
        return stream(collection.find());
    }
}
