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
package org.graylog.plugins.views.startpage.title;

import org.graylog.grn.GRN;
import org.graylog2.lookup.Catalog;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Retrieves titles from cache, using {@link org.graylog2.lookup.Catalog}.
 */
@Singleton
public class StartPageItemTitleRetriever {

    private final Catalog catalog;

    @Inject
    public StartPageItemTitleRetriever(final Catalog catalog) {
        this.catalog = catalog;
    }

    public Optional<String> retrieveTitle(final GRN itemGrn) {
        final Optional<Catalog.Entry> entry = catalog.getEntry(itemGrn);
        final Optional<String> title = entry.map(Catalog.Entry::title);
        if (title.isPresent()) {
            return title;
        } else {
            return entry.map(Catalog.Entry::id);
        }
    }
}
