package org.graylog2.indexer;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog2.indexer.messages.Indexable;
import org.joda.time.DateTime;

import java.util.Map;

import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;
import static org.joda.time.DateTimeZone.UTC;

public class FailureObject implements Indexable {
    private final Map<String, Object> fields;

    public FailureObject(Map<String, Object> fields) {
        this.fields = fields;
    }

    @Override
    public String getId() {
        return getFieldAs(String.class, "id");
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public DateTime getReceiveTime() {
        return null;
    }

    @Override
    public Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @NonNull Meter invalidTimestampMeter) {
        final Map<String, Object> obj = Maps.newHashMapWithExpectedSize(5 + fields.size());

        obj.putAll(fields);
        obj.put("timestamp", buildElasticSearchTimeFormat(getTimestamp().withZone(UTC)));
        obj.put("streams", ImmutableList.of(FAILURES_STREAM_ID));

        return obj;
    }

    @Override
    public DateTime getTimestamp() {
        return getFieldAs(DateTime.class, "timestamp").withZone(UTC);
    }

    public <T> T getFieldAs(final Class<T> T, final String key) throws ClassCastException {
        return T.cast(getField(key));
    }

    public Object getField(final String key) {
        return fields.get(key);
    }
}
