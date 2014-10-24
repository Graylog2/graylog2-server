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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;

import java.io.IOException;

public class StreamRule {
    public enum Type {
        MATCH_EXACTLY(1, "match exactly", "match exactly"),
        MATCH_REGEX(2, "match regular expression", "match regular expression"),
        GREATER_THAN(3, "greater than", "be greater than"),
        SMALLER_THAN(4, "smaller than", "be smaller than"),
        FIELD_PRESENCE(5, "field presence", "be present");

        private final int id;
        private final String shortDesc;
        private final String longDesc;

        Type(int id, String shortDesc, String longDesc) {
            this.id = id;
            this.shortDesc = shortDesc;
            this.longDesc = longDesc;
        }

        public int getId() {
            return id;
        }

        public String getShortDesc() {
            return shortDesc;
        }

        public String getLongDesc() {
            return longDesc;
        }

        public static Type fromInt(int id) {
            for (Type type : Type.values())
                if (type.id == id)
                    return type;
            return null;
        }
    }

    public interface Factory {
        public StreamRule fromSummaryResponse(StreamRuleSummaryResponse srsr);
    }

    private final String id;
    private final String field;
    private final String value;
    private final Type type;
    private final Boolean inverted;
    private final String stream_id;
    private final String contentPack;

    private final StreamService streamService;

    @AssistedInject
    private StreamRule(StreamService streamService, @Assisted StreamRuleSummaryResponse srsr) {
        this.id = srsr.id;
        this.field = srsr.field;
        this.value = srsr.value;
        this.inverted = srsr.inverted;
        this.type = Type.fromInt(srsr.type);
        this.stream_id = srsr.stream_id;
        this.streamService = streamService;
        this.contentPack = srsr.contentPack;
    }

    public String getId() {
        return id;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return (type.equals(Type.FIELD_PRESENCE) ? "" : value);
    }

    public Type getType() {
        return type;
    }

    public Boolean getInverted() {
        return inverted;
    }

    public String getSentenceRepresentation() {
        String sentence;
        try {
            sentence = getType().getLongDesc();
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

        if (this.getType().equals(Type.FIELD_PRESENCE))
            return (this.field + " must " + inverter + this.getSentenceRepresentation());

        return (this.field + " must " + inverter + this.getSentenceRepresentation() + " " + this.value);
    }
}
