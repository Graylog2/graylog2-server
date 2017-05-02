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
package org.graylog2.indexer.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

public class ResultMessage {
    private static final Logger LOG = LoggerFactory.getLogger(ResultMessage.class);

    private Message message;
    private String index;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Multimap<String, Range<Integer>> highlightRanges;

    protected ResultMessage() { /* use factory method */}

    public static ResultMessage parseFromSource(String id, String index, Map<String, Object> message) {
        final ResultMessage m = new ResultMessage();
        m.setMessage(id, message);
        m.setIndex(index);

        return m;
    }

    public static ResultMessage createFromMessage(Message message) {
        ResultMessage m = new ResultMessage();
        m.setMessage(message);
        return m;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setMessage(String id, Map<String, Object> message) {
        Map<String, Object> tmp = Maps.newHashMap();
        tmp.putAll(message);
        tmp.put(Message.FIELD_ID, id);
        if (tmp.containsKey(Message.FIELD_TIMESTAMP)) {
            final Object tsField = tmp.get(Message.FIELD_TIMESTAMP);
            try {
                tmp.put(Message.FIELD_TIMESTAMP, ES_DATE_FORMAT_FORMATTER.parseDateTime(String.valueOf(tsField)));
            } catch (IllegalArgumentException e) {
                // could not parse date string, this is likely a bug, but we will leave the original value alone
                LOG.warn("Could not parse timestamp of message {}", message.get("id"), e);
            }
        }
        this.message = new Message(tmp);
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Message getMessage() {
        return message;
    }

    public String getIndex() {
        return index;
    }
}
