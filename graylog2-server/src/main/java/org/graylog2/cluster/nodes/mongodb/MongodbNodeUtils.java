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
 package org.graylog2.cluster.nodes.mongodb;

import com.mongodb.MongoClient;
import org.bson.Document;

import java.util.Date;

public class MongodbNodeUtils {

    public static final String GRAYLOG_DATABASE_NAME = "graylog";

    public static Long getSlowQueryCount(MongoClient mongoConnection) {
        try {
            // Check if profiling is enabled and query system.profile
            Document profileStatus = mongoConnection.getDatabase(GRAYLOG_DATABASE_NAME).runCommand(new Document("profile", -1));
            int profilingLevel = profileStatus.getInteger("was", 0);

            if (profilingLevel > 0) {
                // Count slow queries from the last 5 minutes
                long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
                Date cutoffTime = new Date(fiveMinutesAgo);

                Document query = new Document("ts", new Document("$gte", cutoffTime))
                        .append("millis", new Document("$gte", 100)); // Queries taking more than 100ms

                return mongoConnection.getDatabase(GRAYLOG_DATABASE_NAME)
                        .getCollection("system.profile")
                        .countDocuments(query);
            }
        } catch (Exception e) {
            // Profiling may not be enabled or accessible
        }
        return null;
    }

    public static double calculateStorageUsedPercent(MongoClient mongoConnection) {
        try {
            final Document dbStats = mongoConnection.getDatabase(GRAYLOG_DATABASE_NAME).runCommand(new Document("dbStats", 1));
            double fsUsedSize = dbStats.getDouble("fsUsedSize");
            double fsTotalSize = dbStats.getDouble("fsTotalSize");
            if (fsTotalSize > 0) {
                return 100.0d * fsUsedSize / fsTotalSize;
            }
        } catch (Exception e) {
            // Stats may not be available or accessible
        }
        return 0.0;
    }
}
