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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.processor.systemnotification.SystemNotificationEventEntityScope;
import org.graylog.plugins.views.search.searchfilters.db.SearchFiltersReFetcher;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ScopedDbService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.search.SearchQuery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.DBUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DBEventDefinitionService extends ScopedDbService<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(DBEventDefinitionService.class);

    public static final String COLLECTION_NAME = "event_definitions";

    private final DBEventProcessorStateService stateService;
    private final EntityOwnershipService entityOwnerShipService;
    private final SearchFiltersReFetcher searchFiltersRefetcher;

    @Inject
    public DBEventDefinitionService(MongoConnection mongoConnection,
                                    MongoJackObjectMapperProvider mapper,
                                    DBEventProcessorStateService stateService,
                                    EntityOwnershipService entityOwnerShipService, EntityScopeService entityScopeService, SearchFiltersReFetcher searchFiltersRefetcher) {
        super(mongoConnection, mapper, EventDefinitionDto.class, COLLECTION_NAME, entityScopeService);
        this.stateService = stateService;
        this.entityOwnerShipService = entityOwnerShipService;
        this.searchFiltersRefetcher = searchFiltersRefetcher;
    }

    public PaginatedList<EventDefinitionDto> searchPaginated(SearchQuery query, Predicate<EventDefinitionDto> filter,
                                                             String sortByField, String sortOrder, int page, int perPage) {
        final DBQuery.Query dbQuery = query.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(sortOrder, sortByField);
        final DBCursor<EventDefinitionDto> cursor = db.find(dbQuery)
                .sort(sortBuilder)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(
                Streams.stream((Iterable<EventDefinitionDto>) cursor)
                        .filter(filter)
                        .map(this::getEventDefinitionWithRefetchedFilters)
                        .collect(Collectors.toList()),
                cursor.count(),
                page,
                perPage
        );
    }

    public EventDefinitionDto saveWithOwnership(EventDefinitionDto eventDefinitionDto, User user) {
        final EventDefinitionDto dto = save(eventDefinitionDto);
        entityOwnerShipService.registerNewEventDefinition(dto.id(), user);
        return dto;
    }

    @Override
    public EventDefinitionDto save(final EventDefinitionDto entity) {
        EventDefinitionDto enrichedWithUpdateDate = entity
                .toBuilder()
                .updatedAt(DateTime.now(DateTimeZone.UTC))
                .build();
        return getEventDefinitionWithRefetchedFilters(super.save(enrichedWithUpdateDate));
    }

    @Override
    public Optional<EventDefinitionDto> get(String id) {
        return super.get(id).map(this::getEventDefinitionWithRefetchedFilters);
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
        db.updateById(new ObjectId(id), new DBUpdate.Builder().set(EventDefinitionDto.FIELD_MATCHED_AT, timeStamp));
    }

    public void updateState(String id, EventDefinition.State state) {
        // Strictly enabling/disabling event definitions does not require a scope check
        db.updateById(new ObjectId(id), new DBUpdate.Builder().set(EventDefinitionDto.FIELD_STATE, state));
    }

    public int deleteUnregister(String id) {
        // Must ensure deletability and mutability before deleting, so that de-registration is only performed if entity exists
        // and is not mutable.
        final EventDefinitionDto dto = get(id).orElseThrow(() -> new IllegalArgumentException("Event Definition not found."));
        ensureDeletability(dto);
        ensureMutability(dto);
        return doDeleteUnregister(id, () -> super.delete(id));
    }

    public int deleteUnregisterImmutable(String id) {
        return doDeleteUnregister(id, () -> super.forceDelete(id));
    }

    private int doDeleteUnregister(String id, Supplier<Integer> deleteSupplier) {
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
        return ImmutableList.copyOf((db.find(DBQuery.is(field, notificationId)).iterator()));
    }

    /**
     * Returns the list of system event definitions
     *
     * @return the matching event definitions
     */
    public List<EventDefinitionDto> getSystemEventDefinitions() {
        return ImmutableList.copyOf((db.find(DBQuery.is(EventDefinitionDto.FIELD_SCOPE, SystemNotificationEventEntityScope.NAME)).iterator()));
    }

    /**
     * Returns the list of event definitions that contain the given value in the specified array field
     */
    @NotNull
    public List<EventDefinitionDto> getByArrayValue(String arrayField, String field, String value) {
        return ImmutableList.copyOf((db.find(DBQuery.elemMatch(arrayField, DBQuery.is(field, value))).iterator()));
    }
}
