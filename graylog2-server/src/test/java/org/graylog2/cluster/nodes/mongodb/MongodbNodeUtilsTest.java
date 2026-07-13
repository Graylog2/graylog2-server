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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongodbNodeUtilsTest {
    private final MongoClient client = mock(MongoClient.class);
    private final MongoDatabase database = mock(MongoDatabase.class);
    @SuppressWarnings("unchecked")
    private final MongoCollection<Document> profileCollection = mock(MongoCollection.class);

    private void dbStats(Document stats) {
        when(client.getDatabase(any())).thenReturn(database);
        when(database.withTimeout(anyLong(), any())).thenReturn(database);
        when(database.runCommand(any(Bson.class))).thenReturn(stats);
    }

    private void slowQueryCount(long count) {
        when(client.getDatabase(any())).thenReturn(database);
        when(database.withTimeout(anyLong(), any())).thenReturn(database);
        when(database.getCollection("system.profile")).thenReturn(profileCollection);
        when(profileCollection.countDocuments(any(Bson.class))).thenReturn(count);
    }

    @Test
    void computesUsedPercentWhenCapacityIsKnown() {
        dbStats(new Document("fsUsedSize", 50.0).append("fsTotalSize", 200.0));

        assertThat(MongodbNodeUtils.calculateStorageUsedPercent(database, Duration.ofSeconds(4))).isEqualTo(25.0);
    }

    @Test
    void timeoutOverloadPropagatesWhenCapacityIsZeroRatherThanReportingAMisleadingZeroPercent() {
        // A partial dbStats document (or a storage engine that reports no filesystem size) must NOT read as a
        // healthy 0% on a health check -- the overload that propagates failures throws so the caller can map it to
        // unknown.
        dbStats(new Document("fsUsedSize", 0.0).append("fsTotalSize", 0.0));

        assertThatThrownBy(() -> MongodbNodeUtils.calculateStorageUsedPercent(database, Duration.ofSeconds(4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no filesystem capacity");
    }

    @Test
    void swallowingOverloadStillReportsZeroWhenCapacityIsZero() {
        // The legacy node-roster callers keep their previous behavior: zero capacity is swallowed to 0.0, not thrown.
        dbStats(new Document("fsUsedSize", 0.0).append("fsTotalSize", 0.0));

        assertThat(MongodbNodeUtils.calculateStorageUsedPercent(client)).isEqualTo(0.0);
    }

    @Test
    void timeoutOverloadPropagatesWhenFilesystemFieldsAreMissingRatherThanThrowingNpe() {
        // A partial dbStats document that omits fsUsedSize/fsTotalSize must surface as the same controlled
        // "capacity unknown" signal -- getDouble returns null for the missing fields, which must not unbox into a
        // NullPointerException.
        dbStats(new Document());

        assertThatThrownBy(() -> MongodbNodeUtils.calculateStorageUsedPercent(database, Duration.ofSeconds(4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no filesystem capacity");
    }

    @Test
    void swallowingOverloadStillReportsZeroWhenFilesystemFieldsAreMissing() {
        // The legacy node-roster callers swallow the missing-field case to 0.0 as well, not an NPE.
        dbStats(new Document());

        assertThat(MongodbNodeUtils.calculateStorageUsedPercent(client)).isEqualTo(0.0);
    }

    @Test
    void zeroSlowQueriesSkipsTheProfilingLevelRead() {
        // The common healthy path (no slow entries -- including a missing or empty system.profile, where
        // countDocuments returns 0) must cost a single round-trip: the profiling level is never read.
        slowQueryCount(0);

        assertThat(MongodbNodeUtils.countLiveSlowQueries(database, Duration.ofSeconds(4))).isZero();
        verify(database, never()).runCommand(any(Bson.class));
    }

    @Test
    void liveSlowQueriesAreCountedWhenProfilingIsEnabled() {
        slowQueryCount(7);
        when(database.runCommand(any(Bson.class))).thenReturn(new Document("was", 1));

        assertThat(MongodbNodeUtils.countLiveSlowQueries(database, Duration.ofSeconds(4))).isEqualTo(7);
    }

    @Test
    void residueEntriesRecordedBeforeProfilingWasDisabledDoNotCount() {
        // Entries can linger in system.profile after profiling is turned off; with profiling OFF they are
        // residue, not a live signal, so the count reads as 0 (healthy) rather than a false warning.
        slowQueryCount(7);
        when(database.runCommand(any(Bson.class))).thenReturn(new Document("was", 0));

        assertThat(MongodbNodeUtils.countLiveSlowQueries(database, Duration.ofSeconds(4))).isZero();
    }

    @Test
    void failsRatherThanGuessesWhenTheCountExhaustsTheBudget() {
        // A zero budget is exhausted by the time the count returns; entries exist, so live-vs-residue cannot be
        // decided. The call must fail (a health caller maps it to unknown) rather than guess a false warning
        // (returning the count) or a false healthy (returning 0) -- and must not issue the second read.
        slowQueryCount(7);

        assertThatThrownBy(() -> MongodbNodeUtils.countLiveSlowQueries(database, Duration.ZERO))
                .isInstanceOf(MongoOperationTimeoutException.class)
                .hasMessageContaining("budget");
        verify(database, never()).runCommand(any(Bson.class));
    }
}
