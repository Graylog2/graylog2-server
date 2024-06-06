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
import org.graylog2.entitygroups.model.EntityType;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.utilities.StringUtils;

import jakarta.inject.Inject;

import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

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

    public Optional<EntityGroup> getByName(String groupName) {
        return dbEntityGroupService.getByName(groupName);
    }

    public List<EntityGroup> getAllForEntity(EntityType type, String entityId) {
        return dbEntityGroupService.getAllForEntity(type, entityId);
    }

    public EntityGroup create(EntityGroup group) {
        return dbEntityGroupService.save(group);
    }

    public EntityGroup update(String id, EntityGroup group) {
        if (dbEntityGroupService.get(id).isEmpty()) {
            throw new NotFoundException("Unable to find entity group to update");
        }
        return dbEntityGroupService.save(group);

    }

    public EntityGroup addEntityToGroup(String groupId, EntityType type, String entityId) {
        final EntityGroup group = requireEntityGroup(groupId);
        return dbEntityGroupService.save(group.addEntity(type, entityId));
    }

    public long delete(String id) {
        return dbEntityGroupService.delete(id);
    }

    public EntityGroup requireEntityGroup(String id) {
        return dbEntityGroupService.get(id)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find entity group to update"));
    }
}
