package org.graylog2.rest.models.messages.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class ResultMessageSummary {
    @JsonProperty("highlight_ranges")
    public abstract Multimap<String, Range<Integer>> highlightRanges();

    @JsonProperty
    public abstract Map<String, Object> message();

    @JsonProperty
    public abstract String index();

    @JsonCreator
    public static ResultMessageSummary create(@JsonProperty("highlight_ranges") Multimap<String, Range<Integer>> highlightRanges,
                                              @JsonProperty("message") Map<String, Object> message,
                                              @JsonProperty("index") String index) {
        return new AutoValue_ResultMessageSummary(highlightRanges, message, index);
    }
}
