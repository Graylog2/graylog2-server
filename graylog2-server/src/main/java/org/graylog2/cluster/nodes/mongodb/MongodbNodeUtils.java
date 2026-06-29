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
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MongodbNodeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MongodbNodeUtils.class);

    public static final int SLOW_QUERIES_THRESHOLD = 100;

    public static ProfilingResult getProfilingResults(MongoClient mongoConnection) {
        return getProfilingResults(mongoConnection, null);
    }

    /**
     * @param timeout optional client-side operation timeout (CSOT). When non-null, the commands run against a
     *                {@code withTimeout} database view so a stuck read fails fast instead of blocking indefinitely
     *                (the Mongo driver's default socket timeout is infinite). Pass {@code null} to leave the
     *                operations bounded only by the client's configured timeouts.
     */
    public static ProfilingResult getProfilingResults(MongoClient mongoConnection, Duration timeout) {
        // Check if profiling is enabled and query system.profile
        final MongoDatabase db = withOptionalTimeout(
                mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME), timeout);
        Document profileStatus = db.runCommand(new Document("profile", -1));
        int profilingLevel = profileStatus.getInteger("was", 0);
        if (profilingLevel > 0) {
            // Count slow queries from the last 5 minutes
            long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
            Date cutoffTime = new Date(fiveMinutesAgo);

            Document query = new Document("ts", new Document("$gte", cutoffTime))
                    .append("millis", new Document("$gte", SLOW_QUERIES_THRESHOLD)); // Queries taking more than 100ms

            final long slowQueries = db.getCollection("system.profile").countDocuments(query);
            return new ProfilingResult(ProfilingLevel.fromNumericalValue(profilingLevel), slowQueries);
        } else {
            return new ProfilingResult(ProfilingLevel.OFF, null);
        }
    }

    public static double calculateStorageUsedPercent(MongoClient mongoConnection) {
        try {
            return storageUsedPercent(mongoConnection, null);
        } catch (Exception e) {
            LOG.warn("Failed to calculate disk usage for mongodb node", e);
            return 0.0;
        }
    }

    /**
     * @param timeout optional client-side operation timeout (CSOT); see
     *                {@link #getProfilingResults(MongoClient, Duration)}. Unlike
     *                {@link #calculateStorageUsedPercent(MongoClient)}, this overload propagates failures instead of
     *                swallowing them as {@code 0.0}, so a caller can surface them. That includes a timeout, a failed
     *                {@code dbStats} command, <em>and</em> a missing/zero {@code fsTotalSize} (no usable capacity
     *                figure) -- the swallowing overload reports the last case as {@code 0.0}, but a caller such as a
     *                health check needs to tell "0% used" apart from "capacity unknown" and map the latter to
     *                {@code unknown} rather than a false "healthy 0%".
     */
    public static double calculateStorageUsedPercent(MongoClient mongoConnection, Duration timeout) {
        return storageUsedPercent(mongoConnection, timeout);
    }

    private static double storageUsedPercent(MongoClient mongoConnection, Duration timeout) {
        final MongoDatabase db = withOptionalTimeout(
                mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME), timeout);
        final Document dbStats = db.runCommand(new Document("dbStats", 1));
        double fsUsedSize = dbStats.getDouble("fsUsedSize");
        double fsTotalSize = dbStats.getDouble("fsTotalSize");
        if (fsTotalSize <= 0) {
            // No usable capacity figure (missing/zero fsTotalSize -- e.g. a partial dbStats document or a storage
            // engine that does not report filesystem size). Signal it rather than returning a misleading 0%: the
            // swallowing overload catches this and reports 0.0 as before, while the timeout overload propagates it
            // so a caller can surface "capacity unknown" instead of a false "healthy 0%".
            throw new IllegalStateException("MongoDB dbStats reported no filesystem capacity (fsTotalSize=" + fsTotalSize + ")");
        }
        return 100.0d * fsUsedSize / fsTotalSize;
    }

    private static MongoDatabase withOptionalTimeout(MongoDatabase db, Duration timeout) {
        return timeout == null ? db : db.withTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
