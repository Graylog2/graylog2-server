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
package org.graylog.testing.completebackend;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog.testing.storage.SearchServer;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.SearchVersion.Distribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.graylog.testing.completebackend.GraylogBackendExtension.MONGODB_VERSION_PROPERTY;
import static org.graylog.testing.completebackend.GraylogBackendExtension.SEARCH_SERVER_DISTRIBUTION_PROPERTY;
import static org.graylog.testing.completebackend.GraylogBackendExtension.SEARCH_SERVER_VERSION_PROPERTY;
import static org.graylog2.shared.utilities.StringUtils.f;

public class BackendServiceVersions {
    private static final Logger LOG = LoggerFactory.getLogger(BackendServiceVersions.class);

    private static final String DEFAULT_MONGODB_VERSION = MongoDBVersion.DEFAULT.version();
    private static final String DEFAULT_SEARCH_SERVER_DISTRIBUTION = Distribution.OPENSEARCH.toString();
    private static final String DEFAULT_SEARCH_SERVER_VERSION = SearchServer.DEFAULT_VERSION.version().toString();
    private static final String SEARCH_SERVER_DISTRIBUTION_VALUES = Arrays.toString(Distribution.values());

    private BackendServiceVersions() {
    }

    public static SearchVersion getSearchServerVersion() {
        final var distribution = defaultIfBlank(System.getProperty(SEARCH_SERVER_DISTRIBUTION_PROPERTY), DEFAULT_SEARCH_SERVER_DISTRIBUTION);
        final var version = defaultIfBlank(System.getProperty(SEARCH_SERVER_VERSION_PROPERTY), DEFAULT_SEARCH_SERVER_VERSION);

        try {
            final var dist = Distribution.valueOf(distribution.toUpperCase(Locale.US));
            final var parsedVersion = switch (dist) {
                case ELASTICSEARCH, OPENSEARCH -> Version.parse(version);
                case DATANODE -> org.graylog2.plugin.Version.CURRENT_CLASSPATH.getVersion();
            };
            return SearchVersion.create(dist, parsedVersion);
        } catch (IllegalArgumentException e) {
            final var msg = f("Invalid search server distribution property: \"%s\". Valid values are: %s",
                    SEARCH_SERVER_DISTRIBUTION_PROPERTY, SEARCH_SERVER_DISTRIBUTION_VALUES);
            LOG.error(msg);
            throw new IllegalArgumentException(msg, e);
        } catch (ParseException e) {
            final var msg = f("Invalid search server version property: \"%s\". Value must be a valid semver version string, e.g. 7.10.2 or 2.19.3",
                    SEARCH_SERVER_VERSION_PROPERTY);
            LOG.error(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    public static MongoDBVersion getMongoDBVersion() {
        return MongoDBVersion.of(requireNonNullElse(System.getProperty(MONGODB_VERSION_PROPERTY), DEFAULT_MONGODB_VERSION));
    }
}
