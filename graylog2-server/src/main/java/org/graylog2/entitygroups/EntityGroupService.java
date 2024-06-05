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

import org.graylog2.entitygroups.events.CategoryDeleted;
import org.graylog2.entitygroups.events.CategoryUpdated;
import org.graylog2.entitygroups.model.EntityGroup;
import org.graylog2.entitygroups.model.DBEntityGroupService;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.utilities.StringUtils;

import jakarta.inject.Inject;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.Optional;
import java.util.function.Predicate;

public class EntityGroupService {

    private final DBEntityGroupService dbEntityGroupService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public EntityGroupService(DBEntityGroupService dbEntityGroupService, ClusterEventBus clusterEventBus) {
        this.dbEntityGroupService = dbEntityGroupService;
        this.clusterEventBus = clusterEventBus;
    }

    public EntityGroup create(String value) {
        Optional<EntityGroup> existingCategory = dbEntityGroupService.getByValue(value);
        if (existingCategory.isPresent()) {
            throw new BadRequestException(StringUtils.f("Category '%s' already exists", value));
        }

        return dbEntityGroupService.save(EntityGroup.builder().category(value).build());
    }

    public PaginatedList<EntityGroup> findPaginated(String query, int page, int perPage, SortOrder order,
                                                       String sortByField, Predicate<EntityGroup> filter) {

        return dbEntityGroupService.findPaginated(query, page, perPage, order.toBsonSort(sortByField), filter);
    }

    public EntityGroup update(String id, String value) {
        EntityGroup existingEntityGroup = dbEntityGroupService.get(id).orElseThrow(
                () -> new NotFoundException("Unable to find category to update"));
        Optional<EntityGroup> categoryByValue = dbEntityGroupService.getByValue(value);

        // Confirm no status with this value already exists
        if (categoryByValue.isPresent()) {
            if (!id.equals(categoryByValue.get().id())) {
                throw new BadRequestException(StringUtils.f("Category '%s' already exists", value));
            } else {
                // The existing value is the same as the updated value so there is nothing to do
                return existingEntityGroup;
            }
        }

        final EntityGroup updated = dbEntityGroupService.save(EntityGroup.builder().id(id).category(value).build());
        clusterEventBus.post(new CategoryUpdated(existingEntityGroup, updated));

        return updated;
    }

    public boolean delete(String id) {
        EntityGroup EntityGroup = dbEntityGroupService.get(id).orElseThrow(() -> new NotFoundException("Unable to find category to delete"));

        boolean wasSuccess = dbEntityGroupService.delete(id) == 1;
        if (wasSuccess) {
            clusterEventBus.post(new CategoryDeleted(EntityGroup));
        }

        return wasSuccess;
    }
}
