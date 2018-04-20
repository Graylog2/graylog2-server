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
package org.graylog2.plugin;

import com.codahale.metrics.Meter;
import com.eaio.uuid.UUID;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;
import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.joda.time.DateTimeZone.UTC;

@NotThreadSafe
public class Message implements Messages {
    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    public static final String FIELD_ID = "_id";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_FULL_MESSAGE = "full_message";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_LEVEL = "level";
    public static final String FIELD_STREAMS = "streams";

    private static final Pattern VALID_KEY_CHARS = Pattern.compile("^[\\w\\.\\-@]*$");
    private static final char KEY_REPLACEMENT_CHAR = '_';

    private static final ImmutableSet<String> GRAYLOG_FIELDS = ImmutableSet.of(
        "gl2_source_node",
        "gl2_source_input",
        // TODO Due to be removed in Graylog 3.x
        "gl2_source_radio",
        "gl2_source_radio_input",

        "gl2_source_collector",
        "gl2_source_collector_input",
        "gl2_remote_ip",
        "gl2_remote_port",
        "gl2_remote_hostname"
    );

    private static final ImmutableSet<String> CORE_MESSAGE_FIELDS = ImmutableSet.of(
        FIELD_MESSAGE,
        FIELD_SOURCE,
        FIELD_TIMESTAMP
    );

    private static final ImmutableSet<String> ES_FIELDS = ImmutableSet.of(
        // ElasticSearch fields.
        FIELD_ID,
        "_ttl",
        "_source",
        "_all",
        "_index",
        "_type",
        "_score"
    );

    public static final ImmutableSet<String> RESERVED_SETTABLE_FIELDS = new ImmutableSet.Builder<String>()
        .addAll(GRAYLOG_FIELDS)
        .addAll(CORE_MESSAGE_FIELDS)
        .build();

    public static final ImmutableSet<String> RESERVED_FIELDS = new ImmutableSet.Builder<String>()
        .addAll(RESERVED_SETTABLE_FIELDS)
        .addAll(ES_FIELDS)
        .build();

    public static final ImmutableSet<String> FILTERED_FIELDS = new ImmutableSet.Builder<String>()
        .addAll(GRAYLOG_FIELDS)
        .addAll(ES_FIELDS)
        .add(FIELD_STREAMS)
        .add(FIELD_FULL_MESSAGE)
        .build();

    private static final ImmutableSet<String> REQUIRED_FIELDS = ImmutableSet.of(
        FIELD_MESSAGE, FIELD_ID
    );

    @Deprecated
    public static final Function<Message, String> ID_FUNCTION = new MessageIdFunction();

    private final Map<String, Object> fields = Maps.newHashMap();
    private Set<Stream> streams = Sets.newHashSet();
    private Set<IndexSet> indexSets = Sets.newHashSet();
    private String sourceInputId;

    // Used for drools to filter out messages.
    private boolean filterOut = false;
    /**
     * The offset the message originally had in the journal it was read from. This will be MIN_VALUE if no journal
     * was involved.
     */
    private long journalOffset = Long.MIN_VALUE;

    private ArrayList<Recording> recordings;

    private com.codahale.metrics.Counter sizeCounter = new com.codahale.metrics.Counter();

    private static final IdentityHashMap<Class<?>, Integer> classSizes = Maps.newIdentityHashMap();
    static {
        classSizes.put(byte.class, 1);
        classSizes.put(Byte.class, 1);

        classSizes.put(char.class, 2);
        classSizes.put(Character.class, 2);

        classSizes.put(short.class, 2);
        classSizes.put(Short.class, 2);

        classSizes.put(boolean.class, 4);
        classSizes.put(Boolean.class, 4);

        classSizes.put(int.class, 4);
        classSizes.put(Integer.class, 4);

        classSizes.put(float.class, 4);
        classSizes.put(Float.class, 4);

        classSizes.put(long.class, 8);
        classSizes.put(Long.class, 8);

        classSizes.put(double.class, 8);
        classSizes.put(Double.class, 8);

        classSizes.put(DateTime.class, 8);
        classSizes.put(Date.class, 8);
        classSizes.put(ZonedDateTime.class, 8);
    }

    public Message(final String message, final String source, final DateTime timestamp) {
        fields.put(FIELD_ID, new UUID().toString());
        addRequiredField(FIELD_MESSAGE, message);
        addRequiredField(FIELD_SOURCE, source);
        addRequiredField(FIELD_TIMESTAMP, timestamp);
    }

    public Message(final Map<String, Object> fields) {
        this((String) fields.get(FIELD_ID), Maps.filterKeys(fields, not(equalTo(FIELD_ID))));
    }

    private Message(String id, Map<String, Object> newFields) {
        Preconditions.checkArgument(id != null, "message id cannot be null");
        fields.put(FIELD_ID, id);
        addFields(newFields);
    }

    public boolean isComplete() {
        for (final String key : REQUIRED_FIELDS) {
            final Object field = getField(key);
            if (field == null || field instanceof String && ((String) field).isEmpty()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Message <{}> is incomplete because the field <{}> is <{}>", fields.get(FIELD_ID), key, field);
                }
                return false;
            }
        }

        return true;
    }

    @Deprecated
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

    public DateTime getTimestamp() {
        return getFieldAs(DateTime.class, FIELD_TIMESTAMP).withZone(UTC);
    }

    public Map<String, Object> toElasticSearchObject(@Nonnull final Meter invalidTimestampMeter) {
        final Map<String, Object> obj = Maps.newHashMapWithExpectedSize(REQUIRED_FIELDS.size() + fields.size());

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            final String key = entry.getKey();
            if (key.equals(FIELD_ID)) {
                continue;
            }

            final Object value = entry.getValue();
            // Elasticsearch does not allow "." characters in keys since version 2.0.
            // See: https://www.elastic.co/guide/en/elasticsearch/reference/2.0/breaking_20_mapping_changes.html#_field_names_may_not_contain_dots
            if (key.contains(".")) {
                final String newKey = key.replace('.', KEY_REPLACEMENT_CHAR);

                // If the message already contains the transformed key, we skip the field and emit a warning.
                // This is still not optimal but better than implementing expensive logic with multiple replacement
                // character options. Conflicts should be rare...
                if (!obj.containsKey(newKey)) {
                    obj.put(newKey, value);
                } else {
                    LOG.warn("Keys must not contain a \".\" character! Ignoring field \"{}\"=\"{}\" in message [{}] - Unable to replace \".\" with a \"{}\" because of key conflict: \"{}\"=\"{}\"",
                        key, value, getId(), KEY_REPLACEMENT_CHAR, newKey, obj.get(newKey));
                    LOG.debug("Full message with \".\" in message key: {}", this);
                }
            } else {
                if (obj.containsKey(key)) {
                    final String newKey = key.replace(KEY_REPLACEMENT_CHAR, '.');
                    // Deliberate warning duplicates because the key with the "." might be transformed before reaching
                    // the duplicate original key with a "_". Otherwise we would silently overwrite the transformed key.
                    LOG.warn("Keys must not contain a \".\" character! Ignoring field \"{}\"=\"{}\" in message [{}] - Unable to replace \".\" with a \"{}\" because of key conflict: \"{}\"=\"{}\"",
                        newKey, fields.get(newKey), getId(), KEY_REPLACEMENT_CHAR, key, value);
                    LOG.debug("Full message with \".\" in message key: {}", this);
                }
                obj.put(key, value);
            }
        }

        obj.put(FIELD_MESSAGE, getMessage());
        obj.put(FIELD_SOURCE, getSource());
        obj.put(FIELD_STREAMS, getStreamIds());

        final Object timestampValue = getField(FIELD_TIMESTAMP);
        DateTime dateTime;
        if (timestampValue instanceof Date) {
            dateTime = new DateTime(timestampValue);
        } else if (timestampValue instanceof DateTime) {
            dateTime = (DateTime) timestampValue;
        } else if (timestampValue instanceof String) {
            // if the timestamp value is a string, we try to parse it in the correct format.
            // we fall back to "now", this avoids losing messages which happen to have the wrong timestamp format
            try {
                dateTime = ES_DATE_FORMAT_FORMATTER.parseDateTime((String) timestampValue);
            } catch (IllegalArgumentException e) {
                LOG.trace("Invalid format for field timestamp '{}' in message {}, forcing to current time.", timestampValue, getId());
                invalidTimestampMeter.mark();
                dateTime = Tools.nowUTC();
            }
        } else {
            // don't allow any other types for timestamp, force to "now"
            LOG.trace("Invalid type for field timestamp '{}' in message {}, forcing to current time.", timestampValue.getClass().getSimpleName(), getId());
            invalidTimestampMeter.mark();
            dateTime = Tools.nowUTC();
        }
        if (dateTime != null) {
            obj.put(FIELD_TIMESTAMP, buildElasticSearchTimeFormat(dateTime.withZone(UTC)));
        }

        return obj;
    }

    // estimate the byte/char length for a field and its value
    static long sizeForField(@Nonnull String key, @Nonnull Object value) {
        return key.length() + sizeForValue(value);
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

        sb.append(" { ");

        final Map<String, Object> filteredFields = Maps.newHashMap(fields);
        filteredFields.remove(FIELD_SOURCE);
        filteredFields.remove(FIELD_MESSAGE);

        Joiner.on(" | ").withKeyValueSeparator(": ").appendTo(sb, filteredFields);

        sb.append(" }");

        return sb.toString();
    }

    public String getMessage() {
        return getFieldAs(String.class, FIELD_MESSAGE);
    }

    public String getSource() {
        return getFieldAs(String.class, FIELD_SOURCE);
    }

    public void setSource(final String source) {
        final Object previousSource = fields.put(FIELD_SOURCE, source);
        updateSize(FIELD_SOURCE, source, previousSource);
    }

    public void addField(final String key, final Object value) {
        addField(key, value, false);
    }

    private void addRequiredField(final String key, final Object value) {
        addField(key, value, true);
    }

    private void addField(final String key, final Object value, final boolean isRequiredField) {
        final String trimmedKey = key.trim();

        // Don't accept protected keys. (some are allowed though lol)
        if ((RESERVED_FIELDS.contains(trimmedKey) && !RESERVED_SETTABLE_FIELDS.contains(trimmedKey)) || !validKey(trimmedKey)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring invalid or reserved key {} for message {}", trimmedKey, getId());
            }
            return;
        }

        final boolean isTimestamp = FIELD_TIMESTAMP.equals(trimmedKey);
        if (isTimestamp && value instanceof Date) {
            final DateTime timestamp = new DateTime(value);
            final Object previousValue = fields.put(FIELD_TIMESTAMP, timestamp);
            updateSize(trimmedKey, timestamp, previousValue);
        } else if (isTimestamp && value instanceof Temporal) {
            final Date date;
            if (value instanceof ZonedDateTime) {
                date = Date.from(((ZonedDateTime) value).toInstant());
            } else if (value instanceof OffsetDateTime) {
                date = Date.from(((OffsetDateTime) value).toInstant());
            } else if (value instanceof LocalDateTime) {
                final LocalDateTime localDateTime = (LocalDateTime) value;
                final ZoneId defaultZoneId = ZoneId.systemDefault();
                final ZoneOffset offset = defaultZoneId.getRules().getOffset(localDateTime);
                date = Date.from(localDateTime.toInstant(offset));
            } else if (value instanceof LocalDate) {
                final LocalDate localDate = (LocalDate) value;
                final LocalDateTime localDateTime = localDate.atStartOfDay();
                final ZoneId defaultZoneId = ZoneId.systemDefault();
                final ZoneOffset offset = defaultZoneId.getRules().getOffset(localDateTime);
                date = Date.from(localDateTime.toInstant(offset));
            } else if (value instanceof Instant) {
                date = Date.from((Instant) value);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unsupported temporal type {}. Using current date and time in message {}.", value.getClass(), getId());
                }
                date = new Date();
            }

            final DateTime timestamp = new DateTime(date);
            final Object previousValue = fields.put(FIELD_TIMESTAMP, timestamp);
            updateSize(trimmedKey, timestamp, previousValue);
        } else if (value instanceof String) {
            final String str = ((String) value).trim();

            if (isRequiredField || !str.isEmpty()) {
                final Object previousValue = fields.put(trimmedKey, str);
                updateSize(trimmedKey, str, previousValue);
            }
        } else if (value != null) {
            final Object previousValue = fields.put(trimmedKey, value);
            updateSize(trimmedKey, value, previousValue);
        }
    }

    private void updateSize(String fieldName, Object newValue, Object previousValue) {
        // don't count internal fields
        if (GRAYLOG_FIELDS.contains(fieldName)) {
            return;
        }
        long newValueSize = 0;
        long oldValueSize = 0;
        final long oldSize = sizeCounter.getCount();
        final int keyLength = fieldName.length();
        // if the field is being removed, also subtract the name's length
        if (newValue == null) {
            sizeCounter.dec(keyLength);
        } else {
            newValueSize = sizeForValue(newValue);
            sizeCounter.inc(newValueSize);
        }
        // if the field is new, also count its name's length
        if (previousValue == null) {
            sizeCounter.inc(keyLength);
        } else {
            oldValueSize = sizeForValue(previousValue);
            sizeCounter.dec(oldValueSize);
        }
        if (LOG.isTraceEnabled()) {
            final long newSize = sizeCounter.getCount();
            LOG.trace("[Message size update][{}] key {}/{}, new/old/change: {}/{}/{} total: {}",
                    getId(), fieldName, keyLength, newValueSize, oldValueSize, newSize - oldSize, newSize);
        }
    }

    static long sizeForValue(@Nonnull Object value) {
        long valueSize;
        if (value instanceof CharSequence) {
            valueSize = ((CharSequence) value).length();
        } else {
            final Integer classSize = classSizes.get(value.getClass());
            valueSize = classSize == null ? 0 : classSize;
        }
        return valueSize;
    }

    public long getSize() {
        return sizeCounter.getCount();
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

    @Deprecated
    public void addStringFields(final Map<String, String> fields) {
        if (fields == null) {
            return;
        }

        for (Map.Entry<String, String> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    @Deprecated
    public void addLongFields(final Map<String, Long> fields) {
        if (fields == null) {
            return;
        }

        for (Map.Entry<String, Long> field : fields.entrySet()) {
            addField(field.getKey(), field.getValue());
        }
    }

    @Deprecated
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
            final Object removedValue = fields.remove(key);
            updateSize(key, null, removedValue);
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

    public Iterable<Map.Entry<String, Object>> getFieldsEntries() {
        return Iterables.unmodifiableIterable(fields.entrySet());
    }

    public int getFieldCount() {
        return fields.size();
    }

    public boolean hasField(String field) {
        return fields.containsKey(field);
    }

    public Set<String> getFieldNames() {
        return Collections.unmodifiableSet(fields.keySet());
    }

    @Deprecated
    public void setStreams(final List<Stream> streams) {
        this.streams = Sets.newHashSet(streams);
    }

    /**
     * Get the streams this message is currently routed to.
     * @return an immutable copy of the current set of assigned streams, empty if no streams have been assigned
     */
    public Set<Stream> getStreams() {
        return ImmutableSet.copyOf(this.streams);
    }

    /**
     * Assign the given stream to this message.
     *
     * @param stream the stream to route this message into
     */
    public void addStream(Stream stream) {
        indexSets.add(stream.getIndexSet());
        if (streams.add(stream)) {
            sizeCounter.inc(8);
            if (LOG.isTraceEnabled()) {
                LOG.trace("[Message size update][{}] stream added: {}", getId(), sizeCounter.getCount());
            }
        }
    }

    /**
     * Assign all of the streams to this message.
     * @param newStreams an iterable of Stream objects
     */
    public void addStreams(Iterable<Stream> newStreams) {
        for (final Stream stream : newStreams) {
            addStream(stream);
        }
    }

    /**
     * Remove the stream assignment from this message.
     * @param stream the stream assignment to remove this message from
     * @return <tt>true</tt> if this message was assigned to the stream
     */
    public boolean removeStream(Stream stream) {
        final boolean removed = streams.remove(stream);

        if (removed) {
            indexSets.clear();
            for (Stream s : streams) {
                indexSets.add(s.getIndexSet());
            }
            sizeCounter.dec(8);
            if (LOG.isTraceEnabled()) {
                LOG.trace("[Message size update][{}] stream removed: {}", getId(), sizeCounter.getCount());
            }
        }

        return removed;
    }

    /**
     * Return the index sets for this message based on the assigned streams.
     *
     * @return index sets
     */
    public Set<IndexSet> getIndexSets() {
        return ImmutableSet.copyOf(this.indexSets);
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getStreamIds() {
        Collection<String> streamField;
        try {
            streamField = getFieldAs(Collection.class, FIELD_STREAMS);
        } catch (ClassCastException e) {
            LOG.trace("Couldn't cast {} to List", FIELD_STREAMS, e);
            streamField = Collections.emptySet();
        }

        final Set<String> streamIds = streamField == null ? new HashSet<>(streams.size()) : new HashSet<>(streamField);
        for (Stream stream : streams) {
            streamIds.add(stream.getId());
        }

        return streamIds;
    }

    public void setFilterOut(final boolean filterOut) {
        this.filterOut = filterOut;
    }

    public boolean getFilterOut() {
        return this.filterOut;
    }

    public void setSourceInputId(String sourceInputId) {
        this.sourceInputId = sourceInputId;
    }

    public String getSourceInputId() {
        return sourceInputId;
    }

    // drools seems to need the "get" prefix
    @Deprecated
    public boolean getIsSourceInetAddress() {
        return fields.containsKey("gl2_remote_ip");
    }

    public InetAddress getInetAddress() {
        if (!fields.containsKey("gl2_remote_ip")) {
            return null;
        }
        final String ipAddr = (String) fields.get("gl2_remote_ip");
        try {
            return InetAddresses.forString(ipAddr);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void setJournalOffset(long journalOffset) {
        this.journalOffset = journalOffset;
    }

    public long getJournalOffset() {
        return journalOffset;
    }

    // helper methods to optionally record timing information per message, useful for debugging or benchmarking
    // not thread safe!
    public void recordTiming(ServerStatus serverStatus, String name, long elapsedNanos) {
        if (shouldNotRecord(serverStatus)) return;
        lazyInitRecordings();
        recordings.add(Recording.timing(name, elapsedNanos));
    }

    public void recordCounter(ServerStatus serverStatus, String name, int counter) {
        if (shouldNotRecord(serverStatus)) return;
        lazyInitRecordings();
        recordings.add(Recording.counter(name, counter));
    }

    public String recordingsAsString() {
        if (hasRecordings()) {
            return Joiner.on(", ").join(recordings);
        }
        return "";
    }

    public boolean hasRecordings() {
        return recordings != null && recordings.size() > 0;
    }

    private void lazyInitRecordings() {
        if (recordings == null) {
            recordings = new ArrayList<>();
        }
    }

    private boolean shouldNotRecord(ServerStatus serverStatus) {
        return !serverStatus.getDetailedMessageRecordingStrategy().shouldRecord(this);
    }

    @Override
    @Nonnull
    public Iterator<Message> iterator() {
        if (getFilterOut()) {
            return Collections.emptyIterator();
        }
        return Iterators.singletonIterator(this);
    }

    public static abstract class Recording {
        static Timing timing(String name, long elapsedNanos) {
            return new Timing(name, elapsedNanos);
        }
        public static Message.Counter counter(String name, int counter) {
            return new Counter(name, counter);
        }

    }

    private static class Timing extends Recording {
        private final String name;
        private final long elapsedNanos;

        Timing(String name, long elapsedNanos) {
            this.name = name;
            this.elapsedNanos = elapsedNanos;
        }

        @Override
        public String toString() {
            return name + ": " + TimeUnit.NANOSECONDS.toMicros(elapsedNanos) + "micros";
        }
    }

    private static class Counter extends Recording {
        private final String name;
        private final int counter;

        public Counter(String name, int counter) {
            this.name = name;
            this.counter = counter;
        }

        @Override
        public String toString() {
            return name + ": " + counter;
        }
    }

    // since we are on Java8 we can replace this with a method reference where needed
    @Deprecated
    public static class MessageIdFunction implements Function<Message, String> {
        @Override
        public String apply(final Message input) {
            return input.getId();
        }
    }
}
