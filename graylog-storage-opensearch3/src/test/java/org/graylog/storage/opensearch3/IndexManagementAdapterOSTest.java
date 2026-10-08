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
package org.graylog.storage.opensearch3;

import org.graylog2.indexer.management.CatShard;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.cat.shards.ShardsRecord;

import static org.assertj.core.api.Assertions.assertThat;

class IndexManagementAdapterOSTest {
    @Test
    void aRelocatingCopyIsStillOnTheNodeItMovesFrom() {
        final CatShard copy = IndexManagementAdapterOS.toCatShard(ShardsRecord.of(r -> r
                .index("graylog_15").shard("0").prirep("p").state("RELOCATING")
                .node("os-dev-node-0 -> 10.40.2.171 2svFqa49RO2JWKoEAwoX_g os-dev-node-1")));

        assertThat(copy).isEqualTo(new CatShard("graylog_15", 0, true, "RELOCATING", "os-dev-node-0", null, null, null));
    }

    @Test
    void anUnassignedReplicaKeepsWhyAndSinceWhen() {
        final CatShard copy = IndexManagementAdapterOS.toCatShard(ShardsRecord.of(r -> r
                .index("logs").shard("1").prirep("r").state("UNASSIGNED")
                .unassignedReason("NODE_LEFT").unassignedAt("2026-10-06T12:48:23.400Z").unassignedDetails("node_left [gone]")));

        assertThat(copy).isEqualTo(new CatShard("logs", 1, false, "UNASSIGNED", null, "NODE_LEFT",
                "2026-10-06T12:48:23.400Z", "node_left [gone]"));
    }
}
