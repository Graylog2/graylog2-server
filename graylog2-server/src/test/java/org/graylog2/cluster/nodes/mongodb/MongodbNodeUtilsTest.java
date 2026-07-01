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
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongodbNodeUtilsTest {
    private final MongoClient client = mock(MongoClient.class);
    private final MongoDatabase database = mock(MongoDatabase.class);

    private void dbStats(Document stats) {
        when(client.getDatabase(any())).thenReturn(database);
        when(database.withTimeout(anyLong(), any())).thenReturn(database);
        when(database.runCommand(any(Bson.class))).thenReturn(stats);
    }

    @Test
    void computesUsedPercentWhenCapacityIsKnown() {
        dbStats(new Document("fsUsedSize", 50.0).append("fsTotalSize", 200.0));

        assertThat(MongodbNodeUtils.calculateStorageUsedPercent(client, Duration.ofSeconds(4))).isEqualTo(25.0);
    }

    @Test
    void timeoutOverloadPropagatesWhenCapacityIsZeroRatherThanReportingAMisleadingZeroPercent() {
        // A partial dbStats document (or a storage engine that reports no filesystem size) must NOT read as a
        // healthy 0% on a health check -- the overload that propagates failures throws so the caller can map it to
        // unknown.
        dbStats(new Document("fsUsedSize", 0.0).append("fsTotalSize", 0.0));

        assertThatThrownBy(() -> MongodbNodeUtils.calculateStorageUsedPercent(client, Duration.ofSeconds(4)))
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

        assertThatThrownBy(() -> MongodbNodeUtils.calculateStorageUsedPercent(client, Duration.ofSeconds(4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no filesystem capacity");
    }

    @Test
    void swallowingOverloadStillReportsZeroWhenFilesystemFieldsAreMissing() {
        // The legacy node-roster callers swallow the missing-field case to 0.0 as well, not an NPE.
        dbStats(new Document());

        assertThat(MongodbNodeUtils.calculateStorageUsedPercent(client)).isEqualTo(0.0);
    }
}
