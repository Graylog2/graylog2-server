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
package org.graylog2.database.nameonly;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Business-layer base for "name-only" entities. Wraps a {@link DBNameOnlyEntityService} with
 * duplicate-value validation on create/update, lookup-or-throw on update/delete, and template
 * hooks for cascading side effects ({@link #onValueRenamed}, {@link #onBeforeDelete}).
 */
public abstract class NameOnlyEntityService<T extends BuildableMongoEntity<T, B>, B extends BuildableMongoEntity.Builder<T, B>> {

    protected final DBNameOnlyEntityService<T, B> dbService;

    protected NameOnlyEntityService(DBNameOnlyEntityService<T, B> dbService) {
        this.dbService = dbService;
    }

    public T create(String value) {
        final String trimmed = requireNonBlank(value);
        if (dbService.getByValue(trimmed).isPresent()) {
            throw new BadRequestException(f("%s '%s' already exists", entityLabel(), trimmed));
        }
        return dbService.save(buildEntity(trimmed));
    }

    public T update(String id, String value) {
        final String trimmed = requireNonBlank(value);
        final T existing = dbService.get(id)
                .orElseThrow(() -> new NotFoundException(f("Unable to find %s to update", entityLabel().toLowerCase(Locale.ROOT))));

        final Optional<T> byValue = dbService.getByValue(trimmed);
        if (byValue.isPresent()) {
            if (!id.equals(byValue.get().id())) {
                throw new BadRequestException(f("%s '%s' already exists", entityLabel(), trimmed));
            }
            // value unchanged — no-op
            return existing;
        }

        final String oldValue = entityValue(existing);
        final T updated = dbService.update(id, trimmed);
        onValueRenamed(oldValue, trimmed);
        return updated;
    }

    /**
     * Deletes the entity and returns the deleted instance so callers can surface the value (e.g. in audit messages).
     */
    public T delete(String id) {
        final T existing = dbService.get(id)
                .orElseThrow(() -> new NotFoundException(f("Unable to find %s to delete", entityLabel().toLowerCase(Locale.ROOT))));
        onBeforeDelete(entityValue(existing));
        dbService.delete(id);
        return existing;
    }

    private String requireNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(f("%s value cannot be empty", entityLabel()));
        }
        return value.trim();
    }

    public PaginatedList<T> findPaginated(String query, int page, int perPage, SortOrder order,
                                          String sortByField, Predicate<T> filter) {
        return dbService.findPaginated(query, page, perPage, order, sortByField, filter);
    }

    public List<T> getAll() {
        try (Stream<T> stream = dbService.streamAll()) {
            return stream.toList();
        }
    }

    /** Build a new entity from just a value (no id). */
    protected abstract T buildEntity(String value);

    /** Read the value field off an entity. */
    protected abstract String entityValue(T entity);

    /** Human-readable, capitalized label used in error messages (e.g. "Tag", "Category"). */
    protected abstract String entityLabel();

    /** Hook fired after a successful rename. Override to cascade to referencing entities. */
    protected void onValueRenamed(String oldValue, String newValue) {
    }

    /** Hook fired before delete commits. Override to clear references on related entities. */
    protected void onBeforeDelete(String value) {
    }
}
