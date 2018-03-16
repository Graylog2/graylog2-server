package org.graylog.plugins.enterprise.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

@AutoValue
@JsonDeserialize(builder = ViewDTO.Builder.class)
@WithBeanGetter
public abstract class ViewDTO {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CREATED_AT = "created_at";

    public static final ImmutableSet<String> SORT_FIELDS = ImmutableSet.of(FIELD_ID, FIELD_TITLE, FIELD_CREATED_AT);

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    // A short, one sentence description of the view
    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    // A longer description of the view, probably including markup text
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_SUMMARY)
        public abstract Builder summary(String summary);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewDTO.Builder()
                    .summary("")
                    .description("")
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public abstract ViewDTO build();
    }
}