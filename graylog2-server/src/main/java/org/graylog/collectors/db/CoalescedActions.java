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
package org.graylog.collectors.db;

import jakarta.annotation.Nullable;

/**
 * The result of coalescing a list of transaction markers into actionable flags.
 *
 * @param recomputeConfig whether to rebuild the collector's config from current fleet state
 * @param newFleetId      non-null if the collector was reassigned to a different fleet
 * @param restart         whether to send a restart command
 * @param runDiscovery    whether to trigger a discovery run
 * @param maxSeq          highest sequence number seen (new value for last_processed_txn_seq)
 */
public record CoalescedActions(boolean recomputeConfig,
                               @Nullable String newFleetId,
                               boolean restart,
                               boolean runDiscovery,
                               long maxSeq) {

    /** No actions needed. */
    public static CoalescedActions empty(long currentSeq) {
        return new CoalescedActions(false, null, false, false, currentSeq);
    }
}
