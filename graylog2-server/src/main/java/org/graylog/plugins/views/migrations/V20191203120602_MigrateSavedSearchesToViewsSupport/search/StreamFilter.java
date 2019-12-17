package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;

@AutoValue
@JsonAutoDetect
abstract class StreamFilter {
    abstract String streamId();

    @JsonValue
    public Map<String, Object> value() {
        return ImmutableMap.of(
                "type", "or",
                "filters", ImmutableSet.of(
                        ImmutableMap.of(
                                "type", "stream",
                                "id", streamId()
                                )
                )
        );
    }

    public static StreamFilter create(String streamId) {
        return new AutoValue_StreamFilter(streamId);
    }
}
