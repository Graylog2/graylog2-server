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
package org.graylog2.migrations;

import com.mongodb.DBCollection;
import com.mongodb.MongoException;
import org.graylog2.database.MongoConnection;
import org.graylog2.system.processing.DBProcessingStatusService;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20190905114400_RemoveOldProcessingStatusIndex extends Migration {
    private final MongoConnection mongoConnection;

    @Inject
    public V20190905114400_RemoveOldProcessingStatusIndex(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }
    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-09-05T11:40:00Z");
    }

    @Override
    public void upgrade() {
        final String OLD_INDEX_NAME = "updated_at_1_input_journal.uncommitted_entries_1_input_journal.written_messages_1m_rate_1";

        DBCollection processing_status = mongoConnection.getDatabase().getCollection(DBProcessingStatusService.COLLECTION_NAME);
        try {
            processing_status.dropIndex(OLD_INDEX_NAME);
        } catch (MongoException ignored) {
            // index was either never created or already deleted
        }
    }
}
