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
package org.graylog2.rest;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.glassfish.hk2.api.Factory;
import org.graylog2.configuration.HttpConfiguration;

/**
 * HK2 factory that produces a request-scoped {@link URIHelper} pre-populated with the external
 * base URI for the current request.
 */
public class URIHelperFactory implements Factory<URIHelper> {

    // Use a Provider so we resolve headers at request time, not at factory-construction time.
    @Context
    private Provider<HttpHeaders> httpHeadersProvider;

    private final HttpConfiguration httpConfiguration;

    @Inject
    public URIHelperFactory(HttpConfiguration httpConfiguration) {
        this.httpConfiguration = httpConfiguration;
    }

    @Override
    public URIHelper provide() {
        final var headers = httpHeadersProvider.get();
        final var baseUri = RestTools.buildExternalUri(
                headers.getRequestHeaders(), httpConfiguration.getHttpExternalUri());
        return new URIHelper(baseUri);
    }

    @Override
    public void dispose(URIHelper instance) {
    }
}
