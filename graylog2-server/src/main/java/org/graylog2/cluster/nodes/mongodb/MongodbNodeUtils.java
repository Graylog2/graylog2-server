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
import com.mongodb.MongoOperationTimeoutException;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.graylog2.shared.utilities.StringUtils.f;

public class MongodbNodeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MongodbNodeUtils.class);

    public static final int SLOW_QUERIES_THRESHOLD = 100;

    // Slow queries recorded further back than this are not counted (they no longer say anything about now).
    private static final Duration PROFILER_LOOKBACK = Duration.ofMinutes(5);

    // countLiveSlowQueries refuses to issue its second round-trip with less than this left of the caller's
    // budget: a read with a degenerate timeout would fail anyway, only slower.
    private static final Duration MIN_REMAINING_BUDGET = Duration.ofMillis(100);

    public static ProfilingResult getProfilingResults(MongoClient mongoConnection) {
        // Check if profiling is enabled and query system.profile
        final MongoDatabase db = mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME);
        Document profileStatus = db.runCommand(new Document("profile", -1));
        int profilingLevel = profileStatus.getInteger("was", 0);
        if (profilingLevel > 0) {
            final long slowQueries = db.getCollection("system.profile").countDocuments(slowQueriesQuery());
            return new ProfilingResult(ProfilingLevel.fromNumericalValue(profilingLevel), slowQueries);
        } else {
            return new ProfilingResult(ProfilingLevel.OFF, null);
        }
    }

    /**
     * Slow-query count for the cluster health check, bounded by a single wall-clock {@code budget} shared across
     * its round-trips (CSOT -- the driver's default socket timeout is infinite, so an unbounded read could block
     * forever). The count runs first -- {@code countDocuments} on a missing or empty {@code system.profile}
     * returns 0 -- so the common healthy path costs one round-trip; the profiling level is read only when entries
     * exist (with whatever budget the count left over), to tell live profiling data from residue recorded before
     * profiling was disabled.
     *
     * @param database the CONFIGURED Graylog database ({@code MongoConnection.getMongoDatabase()}), not a
     *                 hardcoded default name -- on a deployment whose {@code mongodb_uri} names a custom database,
     *                 the profiling level and {@code system.profile} of that database are the ones that matter.
     *                 Reading them requires dbAdmin-level access; an unauthorized read throws (a health caller
     *                 maps it to {@code unknown} with a message naming the missing privilege).
     * @return the number of slow queries in the profiler lookback window, or {@code 0} when profiling is
     *         disabled (residue entries recorded before it was disabled do not count)
     * @throws MongoOperationTimeoutException when the count exhausts the budget before the profiling level could
     *         be read -- live entries cannot be told from residue at that point, so the call fails (for a health
     *         caller: {@code unknown}) rather than guessing a false warning or a false healthy
     */
    public static long countLiveSlowQueries(MongoDatabase database, Duration budget) {
        final long deadlineNanos = System.nanoTime() + budget.toNanos();
        final MongoDatabase db = withOptionalTimeout(database, budget);
        final long slowQueries = db.getCollection("system.profile").countDocuments(slowQueriesQuery());
        if (slowQueries == 0) {
            return 0;
        }

        // Entries exist: read the profiling level to tell live data from residue, bounded by whatever the count
        // left over so the PAIR honors the caller's budget -- a per-read constant would either let the pair
        // overrun the budget or over-throttle a fast first read.
        final Duration remaining = Duration.ofNanos(deadlineNanos - System.nanoTime());
        if (remaining.compareTo(MIN_REMAINING_BUDGET) < 0) {
            throw new MongoOperationTimeoutException(f(
                    "Counting slow queries consumed the %dms budget before the profiling level could be read",
                    budget.toMillis()));
        }
        final Document profileStatus = withOptionalTimeout(db, remaining).runCommand(new Document("profile", -1));
        return profileStatus.getInteger("was", 0) > 0 ? slowQueries : 0;
    }

    /** Slow operations ({@code >= SLOW_QUERIES_THRESHOLD} ms) within the profiler lookback window. */
    private static Document slowQueriesQuery() {
        final Date cutoffTime = new Date(System.currentTimeMillis() - PROFILER_LOOKBACK.toMillis());
        return new Document("ts", new Document("$gte", cutoffTime))
                .append("millis", new Document("$gte", SLOW_QUERIES_THRESHOLD));
    }

    public static double calculateStorageUsedPercent(MongoClient mongoConnection) {
        try {
            return storageUsedPercent(mongoConnection.getDatabase(MongodbClusterCommand.GRAYLOG_DATABASE_NAME));
        } catch (Exception e) {
            LOG.warn("Failed to calculate disk usage for mongodb node", e);
            return 0.0;
        }
    }

    /**
     * @param database the CONFIGURED Graylog database ({@code MongoConnection.getMongoDatabase()}), not a
     *                 hardcoded default name -- {@code dbStats} is unauthorized for a user scoped to a custom
     *                 database when run against a database it has no access to.
     * @param timeout  optional client-side operation timeout (CSOT), so a stuck read fails fast instead of
     *                 blocking indefinitely (the Mongo driver's default socket timeout is infinite). Unlike
     *                 {@link #calculateStorageUsedPercent(MongoClient)}, this overload propagates failures instead of
     *                 swallowing them as {@code 0.0}, so a caller can surface them. That includes a timeout, a failed
     *                 {@code dbStats} command, <em>and</em> a missing/zero {@code fsTotalSize} (no usable capacity
     *                 figure) -- the swallowing overload reports the last case as {@code 0.0}, but a caller such as a
     *                 health check needs to tell "0% used" apart from "capacity unknown" and map the latter to
     *                 {@code unknown} rather than a false "healthy 0%".
     */
    public static double calculateStorageUsedPercent(MongoDatabase database, Duration timeout) {
        return storageUsedPercent(withOptionalTimeout(database, timeout));
    }

    private static double storageUsedPercent(MongoDatabase db) {
        final Document dbStats = db.runCommand(new Document("dbStats", 1));
        // getDouble returns a boxed Double that is null when the field is absent, so read into Double (not the
        // primitive) and null-check before use -- unboxing a missing field into a primitive double would throw a
        // NullPointerException before the capacity handling below could run.
        final Double fsUsedSize = dbStats.getDouble("fsUsedSize");
        final Double fsTotalSize = dbStats.getDouble("fsTotalSize");
        if (fsUsedSize == null || fsTotalSize == null || fsTotalSize <= 0) {
            // No usable capacity figure (missing/zero fsTotalSize, or missing fsUsedSize -- e.g. a partial dbStats
            // document or a storage engine that does not report filesystem size). Signal it rather than returning a
            // misleading 0%: the swallowing overload catches this and reports 0.0 as before, while the timeout
            // overload propagates it so a caller can surface "capacity unknown" instead of a false "healthy 0%".
            throw new IllegalStateException("MongoDB dbStats reported no filesystem capacity (fsUsedSize="
                    + fsUsedSize + ", fsTotalSize=" + fsTotalSize + ")");
        }
        return 100.0d * fsUsedSize / fsTotalSize;
    }

    private static MongoDatabase withOptionalTimeout(MongoDatabase db, Duration timeout) {
        return timeout == null ? db : db.withTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
