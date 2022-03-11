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
package org.graylog2.storage;

import org.graylog2.configuration.validators.SearchVersionRange;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CheckSearchVersionFilter implements ContainerRequestFilter {

    private final Logger LOG = LoggerFactory.getLogger(CheckSearchVersionFilter.class);

    private final ResourceInfo resourceInfo;
    private final ElasticsearchVersionProvider versionProvider;

    @Inject
    public CheckSearchVersionFilter(@Context ResourceInfo resourceInfo, ElasticsearchVersionProvider versionProvider) {
        this.resourceInfo = resourceInfo;
        this.versionProvider = versionProvider;
    }

    private void checkVersion(final RequiresSearchVersion annotation) {

        final SearchVersionRange requiredVersion = SearchVersionRange.of(SearchVersion.Distribution.valueOf(annotation.distribution()), annotation.expression());
        final String message = annotation.message();
        LOG.debug("Checking current version {} satisfies required version {} {}", versionProvider.get(),
                requiredVersion.distribution(), requiredVersion.expression());
        if (!versionProvider.get().satisfies(requiredVersion)) {
            LOG.error(message);
            throw new InternalServerErrorException(message);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (resourceInfo.getResourceMethod().isAnnotationPresent(RequiresSearchVersion.class)) {
            checkVersion(resourceInfo.getResourceMethod().getAnnotation(RequiresSearchVersion.class));
        } else if (resourceInfo.getResourceClass().isAnnotationPresent(RequiresSearchVersion.class)) {
            checkVersion(resourceInfo.getResourceClass().getAnnotation(RequiresSearchVersion.class));
        }
    }
}
