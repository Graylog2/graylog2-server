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
package org.graylog2.indexer.cluster.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.cluster.health.NodeRole.DATA;
import static org.graylog2.indexer.cluster.health.NodeRole.INGEST;
import static org.graylog2.indexer.cluster.health.NodeRole.MASTER_ELIGIBLE;

public class NodeRoleTest {
    @Test
    public void parse() {
        assertThat(NodeRole.parseSymbolString("dim")).containsExactly(DATA, INGEST,
                MASTER_ELIGIBLE);
    }

    @Test
    public void parseUnknown() {
        assertThat(NodeRole.parseSymbolString("d\u00c4")).containsExactly(DATA);
    }
}
