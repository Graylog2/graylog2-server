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
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewResolverDecoder;
import org.graylog2.lookup.Catalog;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;

/**
 * Retrieves titles from cache, using {@link org.graylog2.lookup.Catalog}.
 */
@Singleton
public class StartPageItemTitleRetriever {

    private final Catalog catalog;
    private final Map<String, ViewResolver> viewResolvers;

    @Inject
    public StartPageItemTitleRetriever(final Catalog catalog,
                                       final Map<String, ViewResolver> viewResolvers) {
        this.catalog = catalog;
        this.viewResolvers = viewResolvers;
    }

    public Optional<String> retrieveTitle(final GRN itemGrn, final SearchUser searchUser) {
        if (isSpecialView(itemGrn)) {
            final ViewResolverDecoder decoder = new ViewResolverDecoder(itemGrn.entity());
            if (decoder.isResolverViewId()) {
                final ViewResolver viewResolver = viewResolvers.get(decoder.getResolverName());
                if (viewResolver != null) {
                    Optional<ViewDTO> view = viewResolver.get(decoder.getViewId());
                    if (view.isPresent() && searchUser.canReadView(view.get())) {
                        return Optional.ofNullable(view.get().title());
                    }
                }
            }
        }
        final Optional<Catalog.Entry> entry = catalog.getEntry(itemGrn);
        final Optional<String> title = entry.map(Catalog.Entry::title);
        if (title.isPresent()) {
            return title;
        } else {
            return entry.map(Catalog.Entry::id);
        }


    }

    private boolean isSpecialView(final GRN itemGrn) {
        final GRNType grnType = itemGrn.grnType();
        final String entity = itemGrn.entity();
        return (grnType == GRNTypes.DASHBOARD || grnType == GRNTypes.SEARCH) && entity.contains(ViewResolverDecoder.SEPARATOR);

    }
}
