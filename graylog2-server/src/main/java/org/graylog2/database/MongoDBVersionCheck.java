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
package org.graylog2.database;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.shared.utilities.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class MongoDBVersionCheck {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBVersionCheck.class);
    private static final Version MINIMUM_MONGODB_VERSION = Version.forIntegers(5, 0);

    public static Version getVersion(MongoClient mongoClient) {
        final MongoDatabase adminDb = mongoClient.getDatabase("admin");

        try {
            final Document buildInfoResult = adminDb.runCommand(new BsonDocument("buildinfo", new BsonString("")));

            if (!buildInfoResult.isEmpty()) {
                Object result = buildInfoResult.get("versionArray");
                if (!(result instanceof ArrayList)) {
                    LOG.warn("Couldn't retrieve MongoDB buildInfo");
                    return null;
                }
                @SuppressWarnings("rawtypes")
                final ArrayList versionArray = buildInfoResult.get("versionArray", ArrayList.class);
                if (versionArray == null || versionArray.size() < 3) {
                    LOG.debug("Couldn't retrieve MongoDB version");
                    return null;
                }

                final int majorVersion = (int) versionArray.get(0);
                final int minorVersion = (int) versionArray.get(1);
                final int patchVersion = (int) versionArray.get(2);
                return Version.forIntegers(majorVersion, minorVersion, patchVersion);
            } else {
                LOG.warn("Couldn't retrieve MongoDB buildInfo");
                return null;
            }
        } catch (MongoException e) {
            LOG.warn("Couldn't retrieve MongoDB buildInfo", e);
            return null;
        }
    }

    public static void assertCompatibleVersion(Version mongoVersion) {
        if (mongoVersion != null && mongoVersion.lessThan(MINIMUM_MONGODB_VERSION)) {
            throw new PreflightCheckException(
                    StringUtils.f("You're running MongoDB %s but Graylog requires at least MongoDB %s. Please upgrade.",
                            mongoVersion, MINIMUM_MONGODB_VERSION));
        }
    }
}
