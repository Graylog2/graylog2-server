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
package org.graylog2.categories;

import org.graylog2.categories.events.CategoryDeleted;
import org.graylog2.categories.events.CategoryUpdated;
import org.graylog2.categories.model.Category;
import org.graylog2.categories.model.DBCategoryService;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.utilities.StringUtils;

import jakarta.inject.Inject;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.util.Optional;
import java.util.function.Predicate;

public class CategoryService {

    private final DBCategoryService dbCategoryService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public CategoryService(DBCategoryService dbCategoryService, ClusterEventBus clusterEventBus) {
        this.dbCategoryService = dbCategoryService;
        this.clusterEventBus = clusterEventBus;
    }

    public Category create(String value) {
        Optional<Category> existingCategory = dbCategoryService.getByValue(value);
        if (existingCategory.isPresent()) {
            throw new BadRequestException(StringUtils.f("Category '%s' already exists", value));
        }

        return dbCategoryService.save(Category.builder().category(value).build());
    }

    public PaginatedList<Category> findPaginated(String query, int page, int perPage, SortOrder order,
                                                 String sortByField, Predicate<Category> filter) {

        return dbCategoryService.findPaginated(query, page, perPage, order.toBsonSort(sortByField), filter);
    }

    public Category update(String id, String value) {
        Category existingCategory = dbCategoryService.get(id).orElseThrow(
                () -> new NotFoundException("Unable to find category to update"));
        Optional<Category> categoryByValue = dbCategoryService.getByValue(value);

        // Confirm no status with this value already exists
        if (categoryByValue.isPresent()) {
            if (!id.equals(categoryByValue.get().id())) {
                throw new BadRequestException(StringUtils.f("Category '%s' already exists", value));
            } else {
                // The existing value is the same as the updated value so there is nothing to do
                return existingCategory;
            }
        }

        final Category updated = dbCategoryService.save(Category.builder().id(id).category(value).build());
        clusterEventBus.post(new CategoryUpdated(existingCategory, updated));

        return updated;
    }

    public boolean delete(String id) {
        Category category = dbCategoryService.get(id).orElseThrow(() -> new NotFoundException("Unable to find category to delete"));

        boolean wasSuccess = dbCategoryService.delete(id) == 1;
        if (wasSuccess) {
            clusterEventBus.post(new CategoryDeleted(category));
        }

        return wasSuccess;
    }
}
