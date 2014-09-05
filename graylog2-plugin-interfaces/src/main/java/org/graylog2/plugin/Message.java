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
package org.graylog2.plugin;

import com.eaio.uuid.UUID;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.joda.time.DateTimeZone.UTC;


public class Message {
    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    private static final String FIELD_ID = "_id".intern();
    private static final String FIELD_MESSAGE = "message".intern();
    private static final String FIELD_SOURCE = "source".intern();
    private static final String FIELD_TIMESTAMP = "timestamp".intern();
    private static final String FIELD_STREAMS = "streams".intern();

    private static final Pattern VALID_KEY_CHARS = Pattern.compile("^[\\w\\.\\-]*$");

    public static final ImmutableSet<String> RESERVED_FIELDS = ImmutableSet.of(
            // ElasticSearch fields.
            FIELD_ID,
            "_ttl",
            "_source",
            "_all",
            "_index",
            "_type",
            "_score",

            // Our reserved fields.
            FIELD_MESSAGE,
            FIELD_SOURCE,
            FIELD_TIMESTAMP,
            "gl2_source_node",
            "gl2_source_input",
            "gl2_source_radio",
            "gl2_source_radio_input"
    );

    public static final ImmutableSet<String> RESERVED_SETTABLE_FIELDS = ImmutableSet.of(
            FIELD_MESSAGE,
            FIELD_SOURCE,
            FIELD_TIMESTAMP,
            "gl2_source_node",
            "gl2_source_input",
            "gl2_source_radio",
            "gl2_source_radio_input"
    );

    private static final ImmutableSet<String> REQUIRED_FIELDS = ImmutableSet.of(
            FIELD_MESSAGE, FIELD_SOURCE, FIELD_ID
    );

    public static final Function<Message, String> ID_FUNCTION = new MessageIdFunction();

    private final Map<String, Object> fields = Maps.newHashMap();
    private List<Stream> streams = Lists.newArrayList();
    private MessageInput sourceInput;

    // Used for drools to filter out messages.
    private boolean filterOut = false;
    private InetAddress inetAddress;

    public Message(final String message, final String source, final DateTime timestamp) {
        // Adding the fields directly because they would not be accepted as a reserved fields.
        fields.put(FIELD_ID, new UUID().toString());
        fields.put(FIELD_MESSAGE, message);
        fields.put(FIELD_SOURCE, source);
        fields.put(FIELD_TIMESTAMP, timestamp);
    }

    public Message(final Map<String, Object> fields) {
        addFields(fields);
    }

    public boolean isComplete() {
        for (final String key : REQUIRED_FIELDS) {
            final Object field = getField(key);
            if (field == null || (field instanceof String && ((String) field).isEmpty())) {
                return false;
            }
        }

        return true;
    }

    public String getValidationErrors() {
        final StringBuilder sb = new StringBuilder();

        for (String key : REQUIRED_FIELDS) {
            final Object field = getField(key);
            if (field == null) {
                sb.append(key).append(" is missing, ");
            } else if (field instanceof String && ((String) field).isEmpty()) {
                sb.append(key).append(" is empty, ");
            }
        }
        return sb.toString();
    }

    public String getId() {
        return getFieldAs(String.class, FIELD_ID);
    }

    public Map<String, Object> toElasticSearchObject() {
        final Map<String, Object> obj = Maps.newHashMapWithExpectedSize(REQUIRED_FIELDS.size() + fields.size());

        obj.put(FIELD_MESSAGE, getMessage());
        obj.put(FIELD_SOURCE, getSource());
        obj.putAll(getFields());

        if (getField(FIELD_TIMESTAMP) instanceof DateTime) {
            obj.put(FIELD_TIMESTAMP, buildElasticSearchTimeFormat(((DateTime) getField(FIELD_TIMESTAMP)).withZone(UTC)));
        }

        // Manually converting stream ID to string - caused strange problems without it.
        if (getStreams().isEmpty()) {
            obj.put(FIELD_STREAMS, Collections.emptyList());
        } else {
            final List<String> streamIds = Lists.newArrayListWithCapacity(streams.size());
            for (Stream stream : streams) {
                streamIds.add(stream.getId());
            }
            obj.put(FIELD_STREAMS, streamIds);
        }

        return obj;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("source: ").append(getField(FIELD_SOURCE)).append(" | ");

        final String message = getField(FIELD_MESSAGE).toString().replaceAll("\\n", "").replaceAll("\\t", "");
        sb.append("message: ");

        if (message.length() > 225) {
            sb.append(message.substring(0, 225)).append(" (...)");
        } else {
            sb.append(message);
        }

        final Map<String, Object> filteredFields = Maps.newHashMap(fields);
        filteredFields.remove(FIELD_SOURCE);
        filteredFields.remove(FIELD_MESSAGE);

        Joiner.on(" | ").withKeyValueSeparator(": ").appendTo(sb, filteredFields);

        return sb.toString();
    }

    public String getMessage() {
        return getFieldAs(String.class, FIELD_MESSAGE);
    }

    public String getSource() {
        return getFieldAs(String.class, FIELD_SOURCE);
    }

    public void addField(final String key, final Object value) {
        // Don't accept protected keys. (some are allowed though lol)
        if (RESERVED_FIELDS.contains(key) && !RESERVED_SETTABLE_FIELDS.contains(key) || !validKey(key)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring invalid or reserved key {} for message {}", key, getId());
            }
            return;
        }

        if(value instanceof String) {
            final String str = ((String) value).trim();

            if(!str.isEmpty()) {
                fields.put(key.trim(), str);
            }
        } else if(value != null) {
            fields.put(key.trim(), value);
        }
    }

    public static boolean validKey(final String key) {
        return VALID_KEY_CHARS.matcher(key).matches();
    }

    public void addFields(final Map<String, Object> fields) {
        if (fields == null) {
            return;
        }

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void addStringFields(final Map<String, String> fields) {
        if (fields == null) {
            return;
        }

        for (Map.Entry<String, String> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void addLongFields(final Map<String, Long> fields) {
        if (fields == null) {
            return;
        }

        for (Map.Entry<String, Long> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void addDoubleFields(final Map<String, Double> fields) {
        if (fields == null) {
            return;
        }

        for (Map.Entry<String, Double> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    public void removeField(final String key) {
        if (!RESERVED_FIELDS.contains(key)) {
            fields.remove(key);
        }
    }

    public <T> T getFieldAs(final Class<T> T, final String key) throws ClassCastException {
        return T.cast(getField(key));
    }

    public Object getField(final String key) {
        return fields.get(key);
    }

    public Map<String, Object> getFields() {
        return ImmutableMap.copyOf(fields);
    }

    public void setStreams(final List<Stream> streams) {
        this.streams = Lists.newArrayList(streams);
    }

    public List<Stream> getStreams() {
        return this.streams;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStreamIds() {
        try {
            return Lists.<String>newArrayList(getFieldAs(List.class, FIELD_STREAMS));
        } catch (ClassCastException e) {
            return Collections.emptyList();
        }
    }

    public void setFilterOut(final boolean filterOut) {
        this.filterOut = filterOut;
    }

    public boolean getFilterOut() {
        return this.filterOut;
    }

    public MessageInput getSourceInput() {
        return sourceInput;
    }

    public void setSourceInput(final MessageInput input) {
        this.sourceInput = input;
    }

    // drools seems to need the "get" prefix
    public boolean getIsSourceInetAddress() {
        return inetAddress != null;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public static class MessageIdFunction implements Function<Message, String> {
        @Override
        public String apply(final Message input) {
            return input.getId();
        }
    }
}
