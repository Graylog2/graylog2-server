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
