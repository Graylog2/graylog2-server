/*
 * Copyright 2013 TORCH UG
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
 */
package models;

import com.google.common.collect.Lists;
import models.api.responses.streams.StreamRuleSummaryResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamRule {

    public static final Map<Integer, List<String>> RULES = new HashMap<Integer, List<String>>() {{
        put(1, Lists.newArrayList("match exactly", "match exactly"));
        put(2, Lists.newArrayList("match regular expression", "match regular expression"));
        put(3, Lists.newArrayList("greater than", "be greater than"));
        put(4, Lists.newArrayList("smaller than", "be smaller than"));
    }};

    private final String id;
    private final String field;
    private final String value;
    private final int type;
    private final String stream_id;

    public StreamRule(StreamRuleSummaryResponse srsr) {
        this.id = srsr.id;
        this.field = srsr.field;
        this.value = srsr.value;
        this.type = srsr.type;
        this.stream_id = srsr.stream_id;
    }

    public String getId() {
        return id;
    }

    public String getSentenceRepresentation() {
        String sentence;
        try {
            sentence = RULES.get(this.type).get(1);
        } catch (Exception e) {
            return "unknown";
        }
        return sentence;
    }

    public String getStreamId() {
        return stream_id;
    }

    public String toString() {
        return ("Field " + this.field + " must " + this.getSentenceRepresentation() + " " + this.value);
    }
}
