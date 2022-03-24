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
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class SupportedSearchVersionFilter implements ContainerRequestFilter {

    private final Logger LOG = LoggerFactory.getLogger(SupportedSearchVersionFilter.class);

    private final ResourceInfo resourceInfo;
    private final ElasticsearchVersionProvider versionProvider;

    @Inject
    public SupportedSearchVersionFilter(@Context ResourceInfo resourceInfo, ElasticsearchVersionProvider versionProvider) {
        this.resourceInfo = resourceInfo;
        this.versionProvider = versionProvider;
    }

    private void checkVersion(final SupportedSearchVersion[] annotations) {

        final Set<SearchVersionRange> supportedVersions = Arrays.stream(annotations)
                .map(supportedVersion -> SearchVersionRange.of(supportedVersion.distribution(), supportedVersion.version()))
                .collect(Collectors.toSet());
        final String supportedVersionsString = supportedVersions.stream()
                .map(version -> StringUtils.f("%s %s", version.distribution(), version.expression()))
                .collect(Collectors.joining(", "));
        final SearchVersion currentVersion = versionProvider.get();

        LOG.debug("Checking current version {} satisfies required version [{}]", currentVersion, supportedVersionsString);
        if (!currentVersion.satisfies(supportedVersions)) {
            String errMsg = StringUtils.f("Server search version %s is not compatible with resource. Supported versions: [%s]",
                    currentVersion, supportedVersionsString);
            LOG.error(errMsg);
            throw new InternalServerErrorException(errMsg);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (resourceInfo.getResourceMethod().isAnnotationPresent(SupportedSearchVersion.class) ||
                resourceInfo.getResourceMethod().isAnnotationPresent(SupportedSearchVersions.class)) {
            checkVersion(resourceInfo.getResourceMethod().getAnnotationsByType(SupportedSearchVersion.class));
        } else if (resourceInfo.getResourceClass().isAnnotationPresent(SupportedSearchVersion.class) ||
                resourceInfo.getResourceClass().isAnnotationPresent(SupportedSearchVersions.class)) {
            checkVersion(resourceInfo.getResourceClass().getAnnotationsByType(SupportedSearchVersion.class));
        }
    }
}
