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
package org.graylog2.entitygroups;

import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.entitygroups.model.DBEntityGroupService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;

import jakarta.inject.Inject;

import jakarta.ws.rs.NotFoundException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntityGroupService {
    private final DBEntityGroupService dbEntityGroupService;

    @Inject
    public EntityGroupService(DBEntityGroupService dbEntityGroupService) {
        this.dbEntityGroupService = dbEntityGroupService;
    }

    public PaginatedList<EntityGroup> findPaginated(String query, int page, int perPage, SortOrder order,
                                                    String sortByField, Predicate<EntityGroup> filter) {

        return dbEntityGroupService.findPaginated(query, page, perPage, order.toBsonSort(sortByField), filter);
    }

    public PaginatedList<EntityGroup> findPaginatedForEntity(String type, String entityId, int page, int perPage, SortOrder order,
                                                             String sortByField) {
        return dbEntityGroupService.findPaginatedForEntity(type, entityId, page, perPage, order.toBsonSort(sortByField));
    }

    public Optional<EntityGroup> getByName(String groupName) {
        return dbEntityGroupService.getByName(groupName);
    }

    public Stream<EntityGroup> streamAllForEntity(String type, String entityId) {
        return dbEntityGroupService.streamAllForEntity(type, entityId);
    }

    public Map<String, Collection<EntityGroup>> getAllForEntities(String type, Collection<String> entities) {
        return dbEntityGroupService.getAllForEntities(type, entities).asMap();
    }

    public EntityGroup create(EntityGroup group) {
        return dbEntityGroupService.save(group);
    }

    public EntityGroup update(String id, EntityGroup group) {
        final EntityGroup saved = dbEntityGroupService.update(group.toBuilder().id(id).build());
        if (saved == null) {
            throw new NotFoundException("Unable to find mutable entity group to update");
        }
        return saved;
    }

    public EntityGroup addEntityToGroup(String groupId, String type, String entityId) {
        return dbEntityGroupService.addEntityToGroup(groupId, type, entityId);
    }

    public long delete(String id) {
        return dbEntityGroupService.delete(id);
    }

    public EntityGroup requireEntityGroup(String id) {
        return dbEntityGroupService.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find entity group to update"));
    }
}
