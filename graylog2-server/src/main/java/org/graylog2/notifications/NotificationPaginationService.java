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
package org.graylog2.notifications;

import com.google.common.primitives.Ints;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides paginated access to the existing {@code notifications} MongoDB collection for the entity table UI,
 * plus delete-by-ID operations for entity table row actions.
 * <p>
 * This service operates alongside {@link NotificationServiceImpl} -- both access the same collection.
 * The old service handles publish/fixed/all (the existing caller contract), while this service handles
 * pagination and ID-based deletion (entity table operations).
 */
@Singleton
public class NotificationPaginationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationPaginationService.class);
    private static final String COLLECTION_NAME = "notifications";

    private final MongoCollection<Document> collection;
    private final SystemNotificationRenderService renderService;

    @Inject
    public NotificationPaginationService(MongoConnection mongoConnection,
                                         SystemNotificationRenderService renderService) {
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        this.renderService = renderService;
    }

    /**
     * Returns a paginated list of notifications, with rendered title/description.
     *
     * @param filter  BSON filter (from DbQueryCreator or Filters.empty())
     * @param sort    BSON sort specification
     * @param page    1-indexed page number
     * @param perPage items per page
     * @return paginated result with NotificationSummaryDto entries
     */
    public PaginatedList<NotificationSummaryDto> searchPaginated(Bson filter, Bson sort, int page, int perPage) {
        final long total = collection.countDocuments(filter);

        final List<Document> docs = new ArrayList<>(perPage);
        collection.find(filter)
                .sort(sort)
                .skip(perPage * Math.max(0, page - 1))
                .limit(perPage)
                .into(docs);

        final List<NotificationSummaryDto> dtos = docs.stream()
                .map(this::toSummaryDto)
                .toList();

        return new PaginatedList<>(dtos, Ints.saturatedCast(total), page, perPage);
    }

    /**
     * Finds a single notification by its MongoDB document ID.
     */
    public Optional<NotificationImpl> findById(String id) {
        final Document doc = collection.find(Filters.eq("_id", new ObjectId(id))).first();
        if (doc == null) {
            return Optional.empty();
        }
        return Optional.of(new NotificationImpl(doc.getObjectId("_id"), doc));
    }

    /**
     * Deletes a single notification by its MongoDB document ID.
     *
     * @return true if a document was deleted
     */
    public boolean deleteById(String id) {
        final var result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));
        return result.getDeletedCount() > 0;
    }

    /**
     * Deletes multiple notifications by their MongoDB document IDs.
     *
     * @return number of documents deleted
     */
    public long bulkDelete(List<String> ids) {
        final List<ObjectId> objectIds = ids.stream()
                .map(ObjectId::new)
                .toList();
        final var result = collection.deleteMany(Filters.in("_id", objectIds));
        return result.getDeletedCount();
    }

    /**
     * Returns the number of notifications matching the given filter.
     */
    public long count(Bson filter) {
        return collection.countDocuments(filter);
    }

    @SuppressWarnings("unchecked")
    private NotificationSummaryDto toSummaryDto(Document doc) {
        final String id = doc.getObjectId("_id").toHexString();
        final String type = doc.getString(NotificationImpl.FIELD_TYPE);
        final String key = doc.getString(NotificationImpl.FIELD_KEY);
        final String severity = doc.getString(NotificationImpl.FIELD_SEVERITY);
        final String nodeId = doc.getString(NotificationImpl.FIELD_NODE_ID);
        final String timestamp = doc.getString(NotificationImpl.FIELD_TIMESTAMP);
        final Map<String, Object> details = (Map<String, Object>) doc.get(NotificationImpl.FIELD_DETAILS);

        String title = null;
        String description = null;
        try {
            final var notification = new NotificationImpl(doc.getObjectId("_id"), doc);
            final var rendered = renderService.render(notification);
            title = rendered.title;
            description = rendered.description;
        } catch (Exception e) {
            LOG.warn("Failed to render notification [type={}, key={}]: {}", type, key, e.getMessage());
        }

        return new NotificationSummaryDto(id, type, key, severity, nodeId, title, description, details, timestamp);
    }
}
