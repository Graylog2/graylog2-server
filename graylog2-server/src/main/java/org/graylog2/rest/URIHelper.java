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

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Resolves request-scoped, self-referencing URIs against the server's external base URI.
 * <p>
 * The base URI is derived from {@code HttpConfiguration#getHttpExternalUri()} and the
 * {@code X-Graylog-Server-URL} override header (see {@link RestTools#buildExternalUri}).
 * Inject via {@code @Context URIHelper uriHelper} in JAX-RS resource methods; per-request
 * wiring is provided by {@link URIHelperFactory} / {@link URIHelperBinder}.
 */
public class URIHelper {
    private final URI baseUri;

    public URIHelper(URI baseUri) {
        this.baseUri = requireNonNull(baseUri, "baseUri");
    }

    /**
     * The trailing-slash-terminated base URI used for resolution.
     */
    public URI baseUri() {
        return baseUri;
    }

    /**
     * Resolve a path against the base URI. Relative paths resolve against the base; absolute
     * URIs are returned as-is by {@link URI#resolve(String)}.
     */
    public URI resolve(String path) {
        return baseUri.resolve(path);
    }
}
