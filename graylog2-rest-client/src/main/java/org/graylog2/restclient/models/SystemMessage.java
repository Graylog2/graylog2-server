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
package org.graylog2.restclient.models;

import org.graylog2.restclient.models.api.responses.system.SystemMessageSummaryResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class SystemMessage {

    // This must match the messages returned per page by server or things will go horribly wrong.
    public static final int PER_PAGE = 30;

    private DateTime timestamp;
    private String caller;
    private String content;
    private String nodeId;

    public SystemMessage(SystemMessageSummaryResponse sms) {
        this.timestamp = new DateTime(sms.timestamp, DateTimeZone.UTC);
        this.caller = sms.caller;
        this.content = sms.content;
        this.nodeId = sms.nodeId;
    }

    public SystemMessage(SystemMessage sm) {
        this.timestamp = sm.timestamp;
        this.caller = sm.caller;
        this.content = sm.content;
        this.nodeId = sm.nodeId;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getCaller() {
        return caller;
    }

    public String getContent() {
        return content;
    }

    public String getNodeId() {
        return nodeId;
    }
}
