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
package org.graylog2.shared.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

public class EmbeddingControlFilter implements ContainerResponseFilter {
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private final boolean httpAllowEmbedding;

    enum EmbeddingOptions {
        DENY,
        SAMEORIGIN;
    }

    @Inject
    public EmbeddingControlFilter(@Named("http_allow_embedding") boolean httpAllowEmbedding) {
        this.httpAllowEmbedding = httpAllowEmbedding;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        responseContext.getHeaders().add(X_FRAME_OPTIONS, (httpAllowEmbedding ? EmbeddingOptions.SAMEORIGIN : EmbeddingOptions.DENY).toString());
    }
}
