/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.system.processing;

import org.graylog2.plugin.lifecycles.Lifecycle;
import org.joda.time.DateTime;

/**
 * This is used to track processing status on a single Graylog node.
 */
public interface ProcessingStatusRecorder {
    /**
     * Update the receive time for the "ingest" measurement point. This is done right before a raw messages gets
     * written to the disk journal.
     *
     * @param newTimestamp the new timestamp to record
     */
    void updateIngestReceiveTime(DateTime newTimestamp);

    DateTime getIngestReceiveTime();

    /**
     * Update the receive time for the "post-processing" measurement point. This is done right after all message
     * processors have run.
     *
     * @param newTimestamp the new timestamp to record
     */
    void updatePostProcessingReceiveTime(DateTime newTimestamp);

    DateTime getPostProcessingReceiveTime();

    /**
     * Update receive time for the "post-indexing" measurement point. This is done right after messages
     * have been written to Elasticsearch.
     *
     * @param newTimestamp the new timestamp to record
     */
    void updatePostIndexingReceiveTime(DateTime newTimestamp);

    DateTime getPostIndexingReceiveTime();

    /**
     * Returns the node {@link Lifecycle} status for the node.
     *
     * @return the node lifecycle status
     */
    Lifecycle getNodeLifecycleStatus();

    long getJournalInfoUncommittedEntries();

    double getJournalInfoReadMessages1mRate();

    double getJournalInfoWrittenMessages1mRate();
}
