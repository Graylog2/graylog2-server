/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
