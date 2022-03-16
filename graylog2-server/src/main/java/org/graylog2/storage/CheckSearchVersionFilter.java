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
import org.graylog2.shared.utilities.StringUtils;
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

        final Set<SearchVersionRange> supportedVersions = parseDistributionStrings(annotation.distributions());
        final String message = annotation.message();
        final SearchVersion currentVersion = versionProvider.get();
        LOG.debug("Checking current version {} satisfies required version [{}]", currentVersion,
                String.join(", ", annotation.distributions()));
        if (!currentVersion.satisfies(supportedVersions)) {
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

    private Set<SearchVersionRange> parseDistributionStrings(String[] distributions) throws IllegalArgumentException {
        final Set<SearchVersionRange> validVersions = new HashSet<>();
        final String defaultVersionExpression = ">0";
        for (String distribution : distributions) {
            String[] distroAndVersion = distribution.split(" ");
            if (distroAndVersion.length > 2) {
                throw new IllegalArgumentException(StringUtils.f(
                        "Invalid distribution '%s'. RequiresSearchVersion distribution must be in the form of 'DISTRIBUTION' or 'DISTRIBUTION VERSION'", distribution));
            }
            final SearchVersion.Distribution distro = SearchVersion.Distribution.valueOf(distroAndVersion[0].toUpperCase(Locale.ENGLISH));
            final String version = distroAndVersion.length == 1 ? defaultVersionExpression : distroAndVersion[1];
            validVersions.add(SearchVersionRange.of(distro, version));
        }
        return validVersions;
    }
}
