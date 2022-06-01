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
package org.graylog2.web;

import com.google.common.base.Suppliers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.function.Supplier;

@Singleton
public class IndexHtmlGeneratorProvider implements Provider<IndexHtmlGenerator> {
    private final Supplier<IndexHtmlGenerator> indexHtmlGeneratorSupplier;

    @Inject
    public IndexHtmlGeneratorProvider(Provider<DevelopmentIndexHtmlGenerator> developmentIndexHtmlGeneratorProvider,
                                      Provider<ProductionIndexHtmlGenerator> productionIndexHtmlGeneratorProvider,
                                      @Named("isDevelopmentServer") Boolean isDevelopmentServer) {
        // In development mode we use an external process to provide the web interface.
        // To avoid errors because of missing production web assets, we use a different implementation for
        // generating the "index.html" page.
        final Provider<? extends IndexHtmlGenerator> indexHtmlGeneratorProvider = isDevelopmentServer
                ? developmentIndexHtmlGeneratorProvider
                : productionIndexHtmlGeneratorProvider;

        this.indexHtmlGeneratorSupplier = Suppliers.memoize(indexHtmlGeneratorProvider::get);
    }

    @Override
    public IndexHtmlGenerator get() {
        return this.indexHtmlGeneratorSupplier.get();
    }
}
