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
package org.graylog.collectors;

import org.graylog.collectors.db.CollectorInstanceDTO;

import java.util.Map;
import java.util.Objects;

/**
 * A snapshot of the highest transaction-log sequence number per fleet and per directly-targeted
 * collector instance. Built once from the transaction log (see
 * {@link FleetTransactionLogService#pendingChangesLookup()}) and used to decide whether a collector
 * still has changes to apply — both for the per-instance indicator ({@link #isPending}) and for the
 * "sync status" list filter ({@link CollectorInstanceService#hasPendingChangesFilter}).
 *
 * @param maxByFleetId     highest marker sequence per fleet id (fleet-scoped changes)
 * @param maxByInstanceUid highest marker sequence per collector instance uid (collector-scoped changes)
 */
public record PendingChangesLookup(Map<String, Long> maxByFleetId, Map<String, Long> maxByInstanceUid) {
    public PendingChangesLookup {
        Objects.requireNonNull(maxByFleetId, "maxByFleetId must not be null");
        Objects.requireNonNull(maxByInstanceUid, "maxByInstanceUid must not be null");
    }

    /**
     * Returns whether the given instance has pending changes: {@code true} when the newest marker
     * targeting its current fleet or the instance itself is newer than the instance's last applied
     * sequence ({@link CollectorInstanceDTO#lastProcessedTxnSeq()}).
     */
    public boolean isPending(CollectorInstanceDTO instance) {
        final var maxSeq = Math.max(
                maxByFleetId.getOrDefault(instance.fleetId(), 0L),
                maxByInstanceUid.getOrDefault(instance.instanceUid(), 0L));
        return (maxSeq > instance.lastProcessedTxnSeq());
    }
}
