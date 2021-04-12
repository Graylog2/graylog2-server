package org.graylog.plugins.views.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;
import org.graylog.plugins.views.search.timeranges.OffsetRange;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonTypeName(PartialMessageList.NAME)
@JsonDeserialize(builder = PartialMessageList.Builder.class)
public abstract class PartialMessageList {
    public static final String NAME = "messages";

    public abstract Optional<String> type();

    @JsonProperty
    public abstract Optional<String> id();

    @JsonProperty
    public abstract Optional<String> name();

    @JsonProperty
    public abstract Optional<Filter> filter();

    @JsonProperty
    public abstract Optional<Integer> limit();

    @JsonProperty
    public abstract Optional<Integer> offset();

    @JsonProperty
    public abstract Optional<List<Sort>> sort();

    @JsonProperty
    public abstract Optional<List<Decorator>> decorators();

    @JsonProperty
    public abstract Optional<DerivedTimeRange> timerange();

    @JsonProperty
    public abstract Optional<BackendQuery> query();

    @JsonProperty
    public abstract Optional<Set<String>> streams();

    @JsonCreator
    public static PartialMessageList.Builder builder() {
        return new AutoValue_PartialMessageList.Builder()
                .type(NAME);
    }

    public abstract PartialMessageList.Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static PartialMessageList.Builder createDefault() {
            return builder();
        }

        @JsonProperty
        public abstract PartialMessageList.Builder type(@Nullable String type);

        @JsonProperty
        public abstract PartialMessageList.Builder id(@Nullable String id);

        @JsonProperty
        public abstract PartialMessageList.Builder name(@Nullable String name);

        @JsonProperty
        public abstract PartialMessageList.Builder filter(@Nullable Filter filter);

        @JsonProperty
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(name = AbsoluteRange.ABSOLUTE, value = AbsoluteRange.class),
                @JsonSubTypes.Type(name = RelativeRange.RELATIVE, value = RelativeRange.class),
                @JsonSubTypes.Type(name = KeywordRange.KEYWORD, value = KeywordRange.class),
                @JsonSubTypes.Type(name = OffsetRange.OFFSET, value = OffsetRange.class)
        })
        public PartialMessageList.Builder timerange(@Nullable TimeRange timerange) {
            return timerange(timerange == null ? null : DerivedTimeRange.of(timerange));
        }
        public abstract PartialMessageList.Builder timerange(@Nullable DerivedTimeRange timerange);

        @JsonProperty
        public abstract PartialMessageList.Builder query(@Nullable BackendQuery query);

        @JsonProperty
        public abstract PartialMessageList.Builder streams(@Nullable Set<String> streams);

        @JsonProperty
        public abstract PartialMessageList.Builder limit(@Nullable Integer limit);

        @JsonProperty
        public abstract PartialMessageList.Builder offset(@Nullable Integer offset);

        @JsonProperty
        public abstract PartialMessageList.Builder sort(@Nullable List<Sort> sort);

        @JsonProperty("decorators")
        public PartialMessageList.Builder _decorators(@Nullable List<DecoratorImpl> decorators) {
            return decorators(new ArrayList<>(decorators));
        }

        public abstract PartialMessageList.Builder decorators(@Nullable List<Decorator> decorators);

        public abstract PartialMessageList build();
    }
}
