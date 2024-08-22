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
package org.graylog.events.processor;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.bson.conversions.Bson;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.processor.systemnotification.SystemNotificationEventEntityScope;
import org.graylog.plugins.views.search.searchfilters.db.SearchFiltersReFetcher;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.database.utils.ScopedEntityMongoUtils;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.elemMatch;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.stream;

public class DBEventDefinitionService {
    private static final Logger LOG = LoggerFactory.getLogger(DBEventDefinitionService.class);

    public static final String COLLECTION_NAME = "event_definitions";

    private final MongoCollection<EventDefinitionDto> collection;
    private final MongoUtils<EventDefinitionDto> mongoUtils;
    private final ScopedEntityMongoUtils<EventDefinitionDto> scopedEntityMongoUtils;
    private final MongoPaginationHelper<EventDefinitionDto> paginationHelper;
    private final DBEventProcessorStateService stateService;
    private final EntityOwnershipService entityOwnerShipService;
    private final SearchFiltersReFetcher searchFiltersRefetcher;

    @Inject
    public DBEventDefinitionService(MongoCollections mongoCollections,
                                    DBEventProcessorStateService stateService,
                                    EntityOwnershipService entityOwnerShipService, EntityScopeService entityScopeService, SearchFiltersReFetcher searchFiltersRefetcher) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, EventDefinitionDto.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.stateService = stateService;
        this.entityOwnerShipService = entityOwnerShipService;
        this.searchFiltersRefetcher = searchFiltersRefetcher;
    }

    public PaginatedList<EventDefinitionDto> searchPaginated(SearchQuery query, Predicate<EventDefinitionDto> filter,
                                                             Bson sort, int page, int perPage) {
        final Bson dbQuery = query.toBson();
        final PaginatedList<EventDefinitionDto> list = filter == null ?
                paginationHelper.filter(dbQuery).sort(sort).perPage(perPage).page(page) :
                paginationHelper.filter(dbQuery).sort(sort).perPage(perPage).page(page, filter);
        return new PaginatedList<>(
                list.stream()
                        .map(this::getEventDefinitionWithRefetchedFilters)
                        .collect(Collectors.toList()),
                list.pagination().total(),
                page,
                perPage
        );
    }

    public EventDefinitionDto saveWithOwnership(EventDefinitionDto eventDefinitionDto, User user) {
        final EventDefinitionDto dto = save(eventDefinitionDto);
        entityOwnerShipService.registerNewEventDefinition(dto.id(), user);
        return dto;
    }

    public EventDefinitionDto save(final EventDefinitionDto entity) {
        EventDefinitionDto enrichedWithUpdateDate = entity
                .toBuilder()
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        if (enrichedWithUpdateDate.id() == null) {
            final String id = scopedEntityMongoUtils.create(enrichedWithUpdateDate);
            enrichedWithUpdateDate = enrichedWithUpdateDate.toBuilder().id(id).build();
        } else {
            scopedEntityMongoUtils.update(enrichedWithUpdateDate);
        }
        return getEventDefinitionWithRefetchedFilters(enrichedWithUpdateDate);
    }

    public Optional<EventDefinitionDto> get(String id) {
        return mongoUtils.getById(id).map(this::getEventDefinitionWithRefetchedFilters);
    }

    private EventDefinitionDto getEventDefinitionWithRefetchedFilters(final EventDefinitionDto eventDefinition) {
        final EventProcessorConfig config = eventDefinition.config();
        if (searchFiltersRefetcher.turnedOn() && config instanceof SearchFilterableConfig) {
            final List<UsedSearchFilter> filters = ((SearchFilterableConfig) config).filters();
            final EventProcessorConfig updatedConfig = config.updateFilters(searchFiltersRefetcher.reFetch(filters));
            if (updatedConfig == null) {
                return eventDefinition;
            }
            return eventDefinition.toBuilder().config(updatedConfig).build();
        }
        return eventDefinition;
    }

    public void updateMatchedAt(String id, DateTime timeStamp) {
        collection.findOneAndUpdate(idEq(id), set(EventDefinitionDto.FIELD_MATCHED_AT, timeStamp));
    }

    public void updateState(String id, EventDefinition.State state) {
        // Strictly enabling/disabling event definitions does not require a scope check
        collection.findOneAndUpdate(idEq(id), set(EventDefinitionDto.FIELD_STATE, state));
    }

    public int delete(String id) {
        return scopedEntityMongoUtils.deleteById(id) ? 1 : 0;
    }

    public void forceDelete(String id) {
        scopedEntityMongoUtils.forceDelete(id);
    }

    public long deleteUnregister(String id) {
        // Must ensure deletability and mutability before deleting, so that de-registration is only performed if entity exists
        // and is not mutable.
        final EventDefinitionDto dto = get(id).orElseThrow(() -> new IllegalArgumentException("Event Definition not found."));
        scopedEntityMongoUtils.ensureDeletability(dto);
        scopedEntityMongoUtils.ensureMutability(dto);

        // We've checked that the event definition can be deleted above, so we can safely call forceDelete here.
        return doDeleteUnregister(id, () -> scopedEntityMongoUtils.forceDelete(id));
    }

    public long deleteUnregisterImmutable(String id) {
        return doDeleteUnregister(id, () -> scopedEntityMongoUtils.forceDelete(id));
    }

    private long doDeleteUnregister(String id, Supplier<Long> deleteSupplier) {
        // Deregister event definition.
        try {
            stateService.deleteByEventDefinitionId(id);
        } catch (Exception e) {
            LOG.error("Couldn't delete event processor state for <{}>", id, e);
        }
        entityOwnerShipService.unregisterEventDefinition(id);
        return deleteSupplier.get();
    }

    /**
     * Returns the list of event definitions that is using the given notification ID.
     *
     * @param notificationId the notification ID
     * @return the event definitions with the given notification ID
     */
    public List<EventDefinitionDto> getByNotificationId(String notificationId) {
        final String field = String.format(Locale.US, "%s.%s",
                EventDefinitionDto.FIELD_NOTIFICATIONS,
                EventNotificationConfig.FIELD_NOTIFICATION_ID);
        return stream(collection.find(eq(field, notificationId))).toList();
    }

    /**
     * Returns the list of system event definitions
     *
     * @return the matching event definitions
     */
    public List<EventDefinitionDto> getSystemEventDefinitions() {
        return stream(collection.find(eq(EventDefinitionDto.FIELD_SCOPE, SystemNotificationEventEntityScope.NAME))).toList();
    }

    /**
     * Returns the list of event definitions that contain the given value in the specified array field
     */
    @NotNull
    public List<EventDefinitionDto> getByArrayValue(String arrayField, String field, String value) {
        return stream(collection.find(elemMatch(arrayField, eq(field, value)))).toList();
    }

    public boolean isMutable(EventDefinitionDto eventDefinition) {
        return scopedEntityMongoUtils.isMutable(eventDefinition);
    }

    public Stream<EventDefinitionDto> streamAll() {
        return stream(collection.find());
    }
}
