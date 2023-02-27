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
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.processor.systemnotification.SystemNotificationEventEntityScope;
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
import org.mongojack.DBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DBEventDefinitionService extends ScopedDbService<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(DBEventDefinitionService.class);

    private static final String COLLECTION_NAME = "event_definitions";

    private final DBEventProcessorStateService stateService;
    private final EntityOwnershipService entityOwnerShipService;

    @Inject
    public DBEventDefinitionService(MongoConnection mongoConnection,
                                    MongoJackObjectMapperProvider mapper,
                                    DBEventProcessorStateService stateService,
                                    EntityOwnershipService entityOwnerShipService, EntityScopeService entityScopeService) {
        super(mongoConnection, mapper, EventDefinitionDto.class, COLLECTION_NAME, entityScopeService);
        this.stateService = stateService;
        this.entityOwnerShipService = entityOwnerShipService;
    }

    public PaginatedList<EventDefinitionDto> searchPaginated(SearchQuery query, Predicate<EventDefinitionDto> filter,
                                                             String sortByField, String sortOrder, int page, int perPage) {
        return findPaginatedWithQueryFilterAndSort(query.toDBQuery(), filter,
                getSortBuilder(sortOrder, sortByField), page, perPage);
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
        return super.save(enrichedWithUpdateDate);
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
