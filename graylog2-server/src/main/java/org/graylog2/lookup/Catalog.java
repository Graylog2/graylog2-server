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
package org.graylog2.lookup;

import com.google.common.base.Suppliers;
import org.graylog.grn.GRN;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Singleton
public class Catalog {

    public record Entry(String id, String title) {
    }

    private final Supplier<Map<String, EntityExcerpt>> memoizedExcerptSupplier;

    @Inject
    public Catalog(ContentPackService contentPackService) {
        /*
         * TODO: This approach does not perform and should be replaced.
         * It was implemented as a quick improvement for even worse approach, where getEntityExcerpts() could be invoked for each entry retrieval.
         */
        this.memoizedExcerptSupplier = Suppliers.memoizeWithExpiration(contentPackService::getEntityExcerpts, 5, TimeUnit.SECONDS);
    }

    public Optional<Entry> getEntry(final GRN grn) {
        final String id = grn.entity();
        var catalog = memoizedExcerptSupplier.get();
        var excerpt = catalog.get(id);
        if (excerpt != null) {
            return Optional.of(
                    new Entry(id, excerpt.title())
            );
        } else {
            return Optional.empty();
        }
    }

}
