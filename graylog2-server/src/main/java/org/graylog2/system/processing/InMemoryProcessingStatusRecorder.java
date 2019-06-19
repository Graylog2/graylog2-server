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

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

import static org.joda.time.DateTimeZone.UTC;

@Singleton
public class InMemoryProcessingStatusRecorder implements ProcessingStatusRecorder {
    private final AtomicReference<DateTime> preJournalMaxReceiveTime = new AtomicReference<>(new DateTime(0L, UTC));
    private final AtomicReference<DateTime> postProcessingMaxReceiveTime = new AtomicReference<>(new DateTime(0L, UTC));
    private final AtomicReference<DateTime> postIndexMaxReceiveTime = new AtomicReference<>(new DateTime(0L, UTC));

    @Override
    public DateTime getPreJournalMaxReceiveTime() {
        return preJournalMaxReceiveTime.get();
    }

    @Override
    public DateTime getPostProcessingMaxReceiveTime() {
        return postProcessingMaxReceiveTime.get();
    }

    @Override
    public DateTime getPostIndexingMaxReceiveTime() {
        return postIndexMaxReceiveTime.get();
    }

    @Override
    public void updatePreJournalMaxReceiveTime(DateTime newTimestamp) {
        if (newTimestamp != null) {
            preJournalMaxReceiveTime.updateAndGet(timestamp -> maxTimestamp(timestamp, newTimestamp));
        }
    }

    @Override
    public void updatePostProcessingMaxReceiveTime(DateTime newTimestamp) {
        if (newTimestamp != null) {
            postProcessingMaxReceiveTime.updateAndGet(timestamp -> maxTimestamp(timestamp, newTimestamp));
        }
    }

    @Override
    public void updatePostIndexingMaxReceiveTime(DateTime newTimestamp) {
        if (newTimestamp != null) {
            postIndexMaxReceiveTime.updateAndGet(timestamp -> maxTimestamp(timestamp, newTimestamp));
        }
    }

    private DateTime maxTimestamp(DateTime timestamp, DateTime newTimestamp) {
        return newTimestamp.isAfter(timestamp) ? newTimestamp : timestamp;
    }
}
