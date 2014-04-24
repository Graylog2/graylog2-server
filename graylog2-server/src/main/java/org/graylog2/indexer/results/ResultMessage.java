/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.indexer.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ResultMessage {
    private static final Logger log = LoggerFactory.getLogger(ResultMessage.class);

    public Map<String, Object> message;
    public String index;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Multimap<String, Range<Integer>> highlightRanges;

    protected ResultMessage() { /* use factory method */}
	
	public static ResultMessage parseFromSource(SearchHit hit) {
		ResultMessage m = new ResultMessage();
        m.setMessage(hit.getSource());
		m.setIndex(hit.getIndex());
        m.setHighlightRanges(hit.getHighlightFields());
		return m;
	}
	
	public static ResultMessage parseFromSource(GetResponse r) {
		ResultMessage m = new ResultMessage();
		m.setMessage(r.getSource());
		m.setIndex(r.getIndex());
        return m;
	}
	
	public void setMessage(Map<String, Object> message) {
        this.message = message;
        if (this.message.containsKey("timestamp")) {
            final Object tsField = this.message.get("timestamp");
            try {
                this.message.put("timestamp",
                                 ES_DATE_FORMAT_FORMATTER.parseDateTime(String.valueOf(tsField)));
            } catch (IllegalArgumentException e) {
                // could not parse date string, this is likely a bug, but we will leave the original value alone
                log.warn("Could not parse timestamp of message {}", message.get("id"), e);
            }
        }
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
            log.debug("Highlight positions for message {}: {}", message.get("_id"), highlightRanges);
        }
    }
	
	public void setIndex(String index) {
		this.index = index;
	}

}
