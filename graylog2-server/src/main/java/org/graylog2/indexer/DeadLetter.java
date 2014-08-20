/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.eaio.uuid.UUID;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DeadLetter {

    private final DateTime timestamp;
    private final String id;
    private final BulkItemResponse failure;
    private final Message message;

    public DeadLetter(BulkItemResponse failure, Message message) {
        this.timestamp = Tools.iso8601();
        this.id = new UUID().toString();
        this.failure = failure;
        this.message = message;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getId() {
        return id;
    }

    public BulkItemResponse getFailure() {
        return failure;
    }

    public Message getMessage() {
        return message;
    }

}
