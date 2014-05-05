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
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamRule {

    public interface Factory {
        public StreamRule fromSummaryResponse(StreamRuleSummaryResponse srsr);
    }

    public static final Map<Integer, List<String>> RULES = new HashMap<Integer, List<String>>() {{
        put(1, Lists.newArrayList("match exactly", "match exactly"));
        put(2, Lists.newArrayList("match regular expression", "match regular expression"));
        put(3, Lists.newArrayList("greater than", "be greater than"));
        put(4, Lists.newArrayList("smaller than", "be smaller than"));
        put(5, Lists.newArrayList("field presence", "be present"));
    }};

    private final String id;
    private final String field;
    private final String value;
    private final int type;
    private final Boolean inverted;
    private final String stream_id;

    private final StreamService streamService;

    @AssistedInject
    private StreamRule(StreamService streamService, @Assisted StreamRuleSummaryResponse srsr) {
        this.id = srsr.id;
        this.field = srsr.field;
        this.value = srsr.value;
        this.inverted = srsr.inverted;
        this.type = srsr.type;
        this.stream_id = srsr.stream_id;
        this.streamService = streamService;
    }

    public String getId() {
        return id;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public int getType() {
        return type;
    }

    public Boolean getInverted() {
        return inverted;
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

    public Stream getStream() throws IOException, APIException {
        if (this.streamService == null)
            throw new RuntimeException("foo");
        return this.streamService.get(stream_id);
    }

    public String toString() {
        String inverter = "";
        if (this.getInverted())
            inverter = " not ";

        if (this.getType() == 5)
            return (this.field + " must " + inverter + this.getSentenceRepresentation());

        return (this.field + " must " + inverter + this.getSentenceRepresentation() + " " + this.value);
    }
}
