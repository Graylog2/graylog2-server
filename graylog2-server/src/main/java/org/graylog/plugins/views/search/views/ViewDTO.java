package org.graylog.plugins.views.search.views;

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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = ViewDTO.Builder.class)
@WithBeanGetter
public abstract class ViewDTO {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SEARCH_ID = "search_id";
    public static final String FIELD_PROPERTIES = "properties";
    public static final String FIELD_REQUIRES = "requires";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_DASHBOARD_STATE = "dashboard_state";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

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

    @JsonProperty(FIELD_SEARCH_ID)
    public abstract String searchId();

    @JsonProperty(FIELD_PROPERTIES)
    public abstract ImmutableSet<String> properties();

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_STATE)
    public abstract Map<String, ViewStateDTO> state();

    @JsonProperty(FIELD_DASHBOARD_STATE)
    public abstract ViewDashboardStateDTO dashboardState();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

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

        @JsonProperty(FIELD_SEARCH_ID)
        public abstract Builder searchId(String searchId);

        abstract ImmutableSet.Builder<String> propertiesBuilder();

        @JsonProperty(FIELD_PROPERTIES)
        public Builder properties(Set<String> properties) {
            propertiesBuilder().addAll(properties);
            return this;
        }

        @JsonProperty(FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        @Nullable
        public abstract Builder owner(String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(FIELD_STATE)
        public abstract Builder state(Map<String, ViewStateDTO> state);

        @JsonProperty(FIELD_DASHBOARD_STATE)
        public abstract Builder dashboardState(ViewDashboardStateDTO dashboardState);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewDTO.Builder()
                    .summary("")
                    .description("")
                    .properties(ImmutableSet.of())
                    .dashboardState(ViewDashboardStateDTO.empty())
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public abstract ViewDTO build();
    }
}
