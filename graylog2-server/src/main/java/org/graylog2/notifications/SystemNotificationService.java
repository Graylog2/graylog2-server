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

import org.graylog2.database.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.notifications.SystemNotificationDto.FIELD_IS_READ;
import static org.graylog2.notifications.SystemNotificationDto.FIELD_KEY;
import static org.graylog2.notifications.SystemNotificationDto.FIELD_LAST_CHANGED;
import static org.graylog2.notifications.SystemNotificationDto.FIELD_TRIGGERED_AT;
import static org.graylog2.notifications.SystemNotificationDto.FIELD_TYPE;

@Singleton
public class SystemNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(SystemNotificationService.class);
    static final String COLLECTION_NAME = "system_notifications";

    private final MongoCollection<SystemNotificationDto> collection;
    private final MongoPaginationHelper<SystemNotificationDto> paginationHelper;
    private final MongoUtils<SystemNotificationDto> mongoUtils;
    private final SystemNotificationRenderService renderService;

    @Inject
    public SystemNotificationService(MongoCollections mongoCollections,
                                     SystemNotificationRenderService renderService) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, SystemNotificationDto.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.mongoUtils = mongoCollections.utils(collection);
        this.renderService = renderService;

        collection.createIndexes(List.of(
                // Partial unique: at most one unread per type when key is null
                new IndexModel(
                        Indexes.ascending(FIELD_TYPE),
                        new IndexOptions()
                                .unique(true)
                                .partialFilterExpression(Filters.and(
                                        Filters.eq(FIELD_KEY, null),
                                        Filters.eq(FIELD_IS_READ, false)
                                ))
                ),
                // Partial unique: at most one unread per type+key when key is non-null
                new IndexModel(
                        Indexes.compoundIndex(Indexes.ascending(FIELD_TYPE), Indexes.ascending(FIELD_KEY)),
                        new IndexOptions()
                                .unique(true)
                                .partialFilterExpression(Filters.and(
                                        Filters.type(FIELD_KEY, "string"),
                                        Filters.eq(FIELD_IS_READ, false)
                                ))
                ),
                // Compound index for findAllUnread() and default sort
                new IndexModel(Indexes.compoundIndex(
                        Indexes.ascending(FIELD_IS_READ),
                        Indexes.descending(FIELD_TRIGGERED_AT)
                )),
                // Retention cleanup and sort
                new IndexModel(Indexes.descending(FIELD_TRIGGERED_AT)),
                // Retention cleanup
                new IndexModel(Indexes.ascending(FIELD_LAST_CHANGED))
        ));
    }

    /**
     * Publishes a new notification if no unread entry with the same type+key already exists.
     *
     * @return {@code true} if a new document was inserted, {@code false} if an unread match already existed (dedup no-op)
     */
    public boolean publish(Notification.Type type, String key, Notification.Severity severity,
                           String nodeId, Map<String, Object> details) {
        final String typeStr = type.toString().toLowerCase(Locale.ENGLISH);
        final String severityStr = severity.toString().toLowerCase(Locale.ENGLISH);

        // Check for existing unread entry (dedup)
        final Bson dedupFilter = key != null
                ? Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_KEY, key), Filters.eq(FIELD_IS_READ, false))
                : Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_KEY, null), Filters.eq(FIELD_IS_READ, false));

        if (collection.countDocuments(dedupFilter) > 0) {
            return false;
        }

        // Render title and description
        String title = null;
        String description = null;
        try {
            final var notification = new NotificationBuilder()
                    .addType(type)
                    .addNode(nodeId);
            if (details != null) {
                details.forEach(notification::addDetail);
            }
            final SystemNotificationRenderService.RenderResponse rendered =
                    renderService.render((Notification) notification, SystemNotificationRenderService.Format.PLAINTEXT, null);
            title = rendered.title != null ? rendered.title.strip() : null;
            description = rendered.description != null ? rendered.description.strip() : null;
        } catch (Exception e) {
            LOG.warn("Failed to render notification title/description for type {}: {}", typeStr, e.getMessage());
        }

        final SystemNotificationDto dto = SystemNotificationDto.builder()
                .type(typeStr)
                .key(key)
                .severity(severityStr)
                .nodeId(nodeId)
                .title(title)
                .description(description)
                .details(details != null ? details : Map.of())
                .isRead(false)
                .actor(null)
                .lastChanged(null)
                .triggeredAt(Instant.now())
                .build();

        collection.insertOne(dto);
        return true;
    }

    /**
     * Marks all unread entries matching the given type as read.
     */
    public void markAsRead(Notification.Type type, SystemNotificationDto.Actor actor) {
        final String typeStr = type.toString().toLowerCase(Locale.ENGLISH);
        collection.updateMany(
                Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_IS_READ, false)),
                Updates.combine(
                        Updates.set(FIELD_IS_READ, true),
                        Updates.set(SystemNotificationDto.FIELD_ACTOR, actor),
                        Updates.set(FIELD_LAST_CHANGED, Instant.now())
                )
        );
    }

    /**
     * Marks the unread entry matching the given type+key as read.
     */
    public void markAsRead(Notification.Type type, String key, SystemNotificationDto.Actor actor) {
        final String typeStr = type.toString().toLowerCase(Locale.ENGLISH);
        final Bson filter = key != null
                ? Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_KEY, key), Filters.eq(FIELD_IS_READ, false))
                : Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_KEY, null), Filters.eq(FIELD_IS_READ, false));
        collection.updateMany(
                filter,
                Updates.combine(
                        Updates.set(FIELD_IS_READ, true),
                        Updates.set(SystemNotificationDto.FIELD_ACTOR, actor),
                        Updates.set(FIELD_LAST_CHANGED, Instant.now())
                )
        );
    }

    /**
     * Bulk update: marks entries with the given IDs as read.
     */
    public void markAsRead(List<String> ids, SystemNotificationDto.Actor actor) {
        collection.updateMany(
                MongoUtils.idsIn(ids.stream().map(ObjectId::new).toList()),
                Updates.combine(
                        Updates.set(FIELD_IS_READ, true),
                        Updates.set(SystemNotificationDto.FIELD_ACTOR, actor),
                        Updates.set(FIELD_LAST_CHANGED, Instant.now())
                )
        );
    }

    /**
     * Bulk update: marks entries with the given IDs as unread.
     */
    public void markAsUnread(List<String> ids, SystemNotificationDto.Actor actor) {
        collection.updateMany(
                MongoUtils.idsIn(ids.stream().map(ObjectId::new).toList()),
                Updates.combine(
                        Updates.set(FIELD_IS_READ, false),
                        Updates.set(SystemNotificationDto.FIELD_ACTOR, actor),
                        Updates.set(FIELD_LAST_CHANGED, Instant.now())
                )
        );
    }

    /**
     * Toggles the read state of entries with the given IDs: unread entries become read, read entries become unread.
     */
    public void toggleReadState(List<String> ids, SystemNotificationDto.Actor actor) {
        final List<SystemNotificationDto> entries;
        try (var stream = MongoUtils.stream(
                collection.find(MongoUtils.idsIn(ids.stream().map(ObjectId::new).toList())))) {
            entries = stream.toList();
        }

        final List<String> readIds = entries.stream()
                .filter(SystemNotificationDto::isRead)
                .map(SystemNotificationDto::id)
                .toList();
        final List<String> unreadIds = entries.stream()
                .filter(e -> !e.isRead())
                .map(SystemNotificationDto::id)
                .toList();

        if (!readIds.isEmpty()) {
            markAsUnread(readIds, actor);
        }
        if (!unreadIds.isEmpty()) {
            markAsRead(unreadIds, actor);
        }
    }

    /**
     * Marks all unread entries as read.
     */
    public void markAllAsRead(SystemNotificationDto.Actor actor) {
        collection.updateMany(
                Filters.eq(FIELD_IS_READ, false),
                Updates.combine(
                        Updates.set(FIELD_IS_READ, true),
                        Updates.set(SystemNotificationDto.FIELD_ACTOR, actor),
                        Updates.set(FIELD_LAST_CHANGED, Instant.now())
                )
        );
    }

    /**
     * Paginated query with arbitrary BSON filter and sort.
     */
    public PaginatedList<SystemNotificationDto> searchPaginated(Bson dbQuery, Bson sort, int page, int perPage) {
        return paginationHelper
                .filter(dbQuery)
                .sort(sort)
                .perPage(perPage)
                .page(page);
    }

    /**
     * Returns all unread entries (backward compat for legacy endpoint).
     */
    public List<SystemNotificationDto> findAllUnread() {
        try (var stream = MongoUtils.stream(
                collection.find(Filters.eq(FIELD_IS_READ, false))
                        .sort(Sorts.descending(FIELD_TRIGGERED_AT)))) {
            return stream.toList();
        }
    }

    /**
     * Finds a notification by its ID.
     */
    public Optional<SystemNotificationDto> findById(String id) {
        return mongoUtils.getById(id);
    }

    /**
     * Queries by type+key, any read state. If multiple entries match, returns the unread entry
     * if one exists, otherwise the most recent by triggered_at.
     */
    public Optional<SystemNotificationDto> findByTypeAndKey(Notification.Type type, String key) {
        final String typeStr = type.toString().toLowerCase(Locale.ENGLISH);
        final Bson filter = key != null
                ? Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_KEY, key))
                : Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_KEY, null));

        // Sort by is_read ascending (false < true), then by triggered_at descending.
        // This gives us unread entries first, then most recent read entries.
        final var results = collection.find(filter)
                .sort(Sorts.orderBy(Sorts.ascending(FIELD_IS_READ), Sorts.descending(FIELD_TRIGGERED_AT)))
                .limit(1);

        try (var stream = MongoUtils.stream(results)) {
            return stream.findFirst();
        }
    }

    /**
     * Checks whether any unread entry exists for the given type.
     */
    public boolean hasUnread(Notification.Type type) {
        final String typeStr = type.toString().toLowerCase(Locale.ENGLISH);
        return collection.countDocuments(
                Filters.and(Filters.eq(FIELD_TYPE, typeStr), Filters.eq(FIELD_IS_READ, false))
        ) > 0;
    }

    /**
     * Deletes read entries where last_changed is before the given cutoff.
     * Unread entries are never cleaned by age.
     */
    public long deleteOlderThan(Instant cutoff) {
        return collection.deleteMany(
                Filters.and(
                        Filters.eq(FIELD_IS_READ, true),
                        Filters.lt(FIELD_LAST_CHANGED, cutoff)
                )
        ).getDeletedCount();
    }

    /**
     * If collection count exceeds maxCount, deletes oldest entries by triggered_at.
     * Logs a warning if any deleted entries are unread.
     */
    public long deleteExcess(int maxCount) {
        final long total = collection.countDocuments();
        if (total <= maxCount) {
            return 0;
        }

        final long toDelete = total - maxCount;

        // Find the oldest entries that would be deleted
        final List<SystemNotificationDto> oldest;
        try (var stream = MongoUtils.stream(
                collection.find()
                        .sort(Sorts.ascending(FIELD_TRIGGERED_AT))
                        .limit((int) toDelete))) {
            oldest = stream.toList();
        }

        final long unreadCount = oldest.stream().filter(d -> !d.isRead()).count();
        if (unreadCount > 0) {
            LOG.warn("Safety cap: deleting {} notifications to stay under {} limit, including {} unread entries",
                    toDelete, maxCount, unreadCount);
        }

        final List<String> idsToDelete = oldest.stream().map(SystemNotificationDto::id).toList();
        return collection.deleteMany(MongoUtils.stringIdsIn(idsToDelete)).getDeletedCount();
    }
}
