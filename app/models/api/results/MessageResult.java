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
package models.api.results;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;

public class MessageResult {
    private final static Set<String> HIDDEN_FIELDS = ImmutableSet.of(
            "_id",
            "timestamp",
            "streams",
            "gl2_source_input",
            "gl2_source_node",
            "gl2_source_radio",
            "gl2_source_radio_input"
    );

    private final Map<String, Object> fields;
    private final String index;
    private final String id;
    private final String timestamp;
    private final String sourceNodeId;
    private final String sourceInputId;
    private final String sourceRadioId;
    private final String sourceRadioInputId;
    private final List<String> streamIds;

    public MessageResult(Map<String, Object> message, String index) {
        // this comparator sorts fields alphabetically, but always leaves full_message at the end.
        // it really is interface, but I don't want to put it into the template either.
        // doing it here also means we don't have to copy the entire map when sorting...
        fields = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                // full_message is always "larger" than anything else, except itself
                final boolean rightIsFullMessage = right.equals("full_message");
                final boolean leftisFullMessage = left.equals("full_message");
                if (leftisFullMessage) {
                    if (rightIsFullMessage) {
                        return 0; // SAME
                    } else {
                        return 1; // LEFT is larger
                    }
                } else {
                    if (rightIsFullMessage) {
                        return -1; // RIGHT is larger
                    }
                }
                return left.compareTo(right);
            }
        });
        for (Map.Entry<String, Object> f : message.entrySet()) {
            if (HIDDEN_FIELDS.contains(f.getKey())) {
                continue;
            }

            fields.put(f.getKey(), f.getValue());
        }

        this.id = (String) message.get("_id");
        this.timestamp = (String) message.get("timestamp");

        this.sourceNodeId = (String) message.get("gl2_source_node");
        this.sourceInputId = (String) message.get("gl2_source_input");
        this.index = index;
        this.streamIds = (List<String>) message.get("streams");

        if(message.containsKey("gl2_source_radio")) {
            sourceRadioId = (String) message.get("gl2_source_radio");
            sourceRadioInputId = (String) message.get("gl2_source_radio_input");
        } else {
            sourceRadioId = null;
            sourceRadioInputId = null;
        }
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Map<String, Object> getFormattedFields() {
        Map<String, Object> formatted = Maps.newHashMap();
        for (Map.Entry<String, Object> field : getFields().entrySet()) {
            String key = field.getKey();
            Object value = field.getValue();

            // Get rid of .0 of doubles. 9001.0 becomes "9001", 9001.25 becomes "9001.25"
            if(value instanceof Double) {
                Double d = (Double) value;
                value = (d.longValue() == d ? "" + d.longValue() : "" + d);
            }

            formatted.put(key, value);
        }

        return formatted;
    }

    public String getIndex() {
        return index;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getSourceInputId() {
        return sourceInputId;
    }

    public boolean viaRadio() {
        return sourceRadioId != null;
    }

    public String getSourceRadioId() {
        return sourceRadioId;
    }

    public String getSourceRadioInputId() {
        return sourceRadioInputId;
    }

    public List<String> getStreamIds() {
        if (this.streamIds != null) {
            return this.streamIds;
        } else {
            return Lists.newArrayList();
        }
    }

    /*public List<Stream> getStreams() throws IOException, APIException {
        List<Stream> result = Lists.newArrayList();
        for (String streamId : getStreamIds()) {
            System.out.println("Fetching stream " + streamId);
            Stream stream = streamService.get(streamId);
            if (stream != null)
                result.add(stream);
        }
        return result;
    }*/

}
