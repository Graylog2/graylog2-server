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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.highlight.HighlightField;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ResultMessage {
    private static final Logger LOG = LoggerFactory.getLogger(ResultMessage.class);

    private Message message;
    private String index;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Multimap<String, Range<Integer>> highlightRanges;

    protected ResultMessage() { /* use factory method */}

    public static ResultMessage parseFromSource(SearchHit hit) {
        ResultMessage m = new ResultMessage();
        Map<String, Object> message;

        // There is no _source field if addFields is used for the request. Just use the returned fields in that case.
        if (hit.getSource() != null) {
            message = hit.getSource();
        } else {
            message = Maps.newHashMap();
            for (Map.Entry<String, SearchHitField> o : hit.fields().entrySet()) {
                message.put(o.getKey(), o.getValue().getValue());
            }
        }
        m.setMessage(hit.getId(), message);
        m.setIndex(hit.getIndex());
        m.setHighlightRanges(hit.getHighlightFields());
        return m;
    }

    public static ResultMessage parseFromSource(GetResponse r) {
        ResultMessage m = new ResultMessage();
        m.setMessage(r.getId(), r.getSource());
        m.setIndex(r.getIndex());
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
        tmp.put("_id", id);
        if (tmp.containsKey("timestamp")) {
            final Object tsField = tmp.get("timestamp");
            try {
                tmp.put("timestamp", ES_DATE_FORMAT_FORMATTER.parseDateTime(String.valueOf(tsField)));
            } catch (IllegalArgumentException e) {
                // could not parse date string, this is likely a bug, but we will leave the original value alone
                LOG.warn("Could not parse timestamp of message {}", message.get("id"), e);
            }
        }
        this.message = new Message(tmp);
    }

    public void setHighlightRanges(Map<String, HighlightField> highlightFields) {
        if (!highlightFields.isEmpty()) {
            highlightRanges = ArrayListMultimap.create();
            for (Map.Entry<String, HighlightField> hlEntry : highlightFields.entrySet()) {
                final HighlightField highlight = hlEntry.getValue();
                final String s = highlight.fragments()[0].toString();

                int pos = 0;
                int cutChars = 0;
                while (true) {
                    int startIdx = s.indexOf("<em>", pos);
                    if (startIdx == -1) {
                        break;
                    }
                    final int endIdx = s.indexOf("</em>", startIdx);
                    final Range<Integer> highlightPosition = Range.closedOpen(startIdx - cutChars, endIdx - cutChars - 4);
                    pos = endIdx;
                    cutChars += 9;
                    highlightRanges.put(hlEntry.getKey(), highlightPosition);
                }
            }
            LOG.debug("Highlight positions for message {}: {}", message.getId(), highlightRanges);
        }
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
