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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

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
@JsonDeserialize(builder = View.Builder.class)
@WithBeanGetter
abstract class View {
    enum Type {
        SEARCH,
        DASHBOARD
    }

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_OWNER = "owner";

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    abstract String id();

    @JsonProperty(FIELD_TYPE)
    abstract Type type();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    abstract String title();

    @JsonProperty(FIELD_SUMMARY)
    abstract String summary();

    @JsonProperty(FIELD_DESCRIPTION)
    abstract String description();

    @JsonProperty(FIELD_SEARCH_ID)
    abstract String searchId();

    @JsonProperty(FIELD_PROPERTIES)
    abstract Set<String> properties();

    @JsonProperty(FIELD_REQUIRES)
    abstract Map<String, Object> requires();

    @JsonProperty(FIELD_STATE)
    abstract Map<String, ViewState> state();

    @JsonProperty(FIELD_OWNER)
    abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    abstract DateTime createdAt();

    static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        abstract Builder id(String id);

        @JsonProperty(FIELD_TYPE)
        abstract Builder type(Type type);

        @JsonProperty(FIELD_TITLE)
        abstract Builder title(String title);

        @JsonProperty(FIELD_SUMMARY)
        abstract Builder summary(String summary);

        @JsonProperty(FIELD_DESCRIPTION)
        abstract Builder description(String description);

        @JsonProperty(FIELD_SEARCH_ID)
        abstract Builder searchId(String searchId);

        @JsonProperty(FIELD_PROPERTIES)
        abstract Builder properties(Set<String> properties);

        @JsonProperty(FIELD_REQUIRES)
        abstract Builder requires(Map<String, Object> requirements);

        @JsonProperty(FIELD_OWNER)
        @Nullable
        abstract Builder owner(String owner);

        @JsonProperty(FIELD_CREATED_AT)
        abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(FIELD_STATE)
        abstract Builder state(Map<String, ViewState> state);

        @JsonCreator
        static Builder create() {
            return new AutoValue_View.Builder()
                    .summary("")
                    .description("")
                    .properties(ImmutableSet.of())
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        abstract View build();
    }
}
