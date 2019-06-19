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

import org.joda.time.DateTime;

/**
 * This is used to track processing status on a single Graylog node.
 */
public interface ProcessingStatusRecorder {
    /**
     * Update max receive time for the "pre-journal" measurement point. This is done right before a raw messages gets
     * written to the disk journal.
     *
     * @param newTimestamp the new timestamp to record
     */
    void updatePreJournalMaxReceiveTime(DateTime newTimestamp);

    DateTime getPreJournalMaxReceiveTime();

    /**
     * Update max receive time for the "post-processing" measurement point. This is done right after all message
     * processors have run.
     *
     * @param newTimestamp the new timestamp to record
     */
    void updatePostProcessingMaxReceiveTime(DateTime newTimestamp);

    DateTime getPostProcessingMaxReceiveTime();

    /**
     * Update max receive time for the "post-indexing" measurement point. This is done right after messages
     * have been written to Elasticsearch.
     *
     * @param newTimestamp the new timestamp to record
     */
    void updatePostIndexingMaxReceiveTime(DateTime newTimestamp);

    DateTime getPostIndexingMaxReceiveTime();
}
