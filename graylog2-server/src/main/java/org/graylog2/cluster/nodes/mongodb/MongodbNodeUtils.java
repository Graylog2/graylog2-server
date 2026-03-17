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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class MongodbNodeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MongodbNodeUtils.class);

    public static final int SLOW_QUERIES_THRESHOLD = 100;

    public static ProfilingResult getProfilingResults(MongoClient mongoConnection) {
        // Check if profiling is enabled and query system.profile
        Document profileStatus = mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME).runCommand(new Document("profile", -1));
        int profilingLevel = profileStatus.getInteger("was", 0);
        if (profilingLevel > 0) {
            // Count slow queries from the last 5 minutes
            long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
            Date cutoffTime = new Date(fiveMinutesAgo);

            Document query = new Document("ts", new Document("$gte", cutoffTime))
                    .append("millis", new Document("$gte", SLOW_QUERIES_THRESHOLD)); // Queries taking more than 100ms

            final long slowQueries = mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME)
                    .getCollection("system.profile")
                    .countDocuments(query);
            return new ProfilingResult(ProfilingLevel.fromNumericalValue(profilingLevel), slowQueries);
        } else {
            return new ProfilingResult(ProfilingLevel.OFF, null);
        }
    }

    public static double calculateStorageUsedPercent(MongoClient mongoConnection) {
        try {
            final Document dbStats = mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME).runCommand(new Document("dbStats", 1));
            double fsUsedSize = dbStats.getDouble("fsUsedSize");
            double fsTotalSize = dbStats.getDouble("fsTotalSize");
            if (fsTotalSize > 0) {
                return 100.0d * fsUsedSize / fsTotalSize;
            }
        } catch (Exception e) {
            LOG.warn("Failed to calculate disk usage for mongodb node", e);
        }
        return 0.0;
    }
}
