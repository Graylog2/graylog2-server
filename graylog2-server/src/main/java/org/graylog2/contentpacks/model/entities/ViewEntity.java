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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = ViewEntity.Builder.class)
@WithBeanGetter
public abstract class ViewEntity {
    public enum Type {
        SEARCH,
        DASHBOARD
    }

    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SEARCH = "search";
    public static final String FIELD_PROPERTIES = "properties";
    public static final String FIELD_REQUIRES = "requires";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_OWNER = "owner";

    @JsonProperty(FIELD_TYPE)
    public abstract Type type();

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract ValueReference title();

    // A short, one sentence description of the view
    @JsonProperty(FIELD_SUMMARY)
    public abstract ValueReference summary();

    // A longer description of the view, probably including markup text
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract ValueReference description();

    @JsonProperty(FIELD_SEARCH)
    public abstract SearchEntity search();

    @JsonProperty(FIELD_PROPERTIES)
    public abstract ImmutableSet<String> properties();

    @JsonProperty(FIELD_REQUIRES)
    public abstract Map<String, PluginMetadataSummary> requires();

    @JsonProperty(FIELD_STATE)
    public abstract Map<String, ViewStateEntity> state();

    @JsonProperty(FIELD_OWNER)
    public abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    public ViewDTO.Type dtoType() {
        switch (type()) {
            case SEARCH: return ViewDTO.Type.SEARCH;
            case DASHBOARD: return ViewDTO.Type.DASHBOARD;
            default:
                throw new IllegalArgumentException("Unsupported view type:" + type());
        }
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(Type type);

        public Builder type(ViewDTO.Type type) {
            switch (type) {
                case SEARCH:
                    type(Type.SEARCH);
                    break;
                case DASHBOARD:
                    type(Type.DASHBOARD);
                    break;
            }
            return this;
        }

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(ValueReference title);

        @JsonProperty(FIELD_SUMMARY)
        public abstract Builder summary(ValueReference summary);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(ValueReference description);

        @JsonProperty(FIELD_SEARCH)
        public abstract Builder search(SearchEntity search);

        abstract ImmutableSet.Builder<String> propertiesBuilder();

        @JsonProperty(FIELD_PROPERTIES)
        public Builder properties(Set<String> properties) {
            propertiesBuilder().addAll(properties);
            return this;
        }

        @JsonProperty(FIELD_REQUIRES)
        public abstract Builder requires(Map<String, PluginMetadataSummary> requirements);

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(FIELD_STATE)
        public abstract Builder state(Map<String, ViewStateEntity> state);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewEntity.Builder()
                    .type(Type.DASHBOARD)
                    .summary(ValueReference.of(""))
                    .description(ValueReference.of(""))
                    .properties(ImmutableSet.of())
                    .requires(Collections.emptyMap())
                    .createdAt(DateTime.now(DateTimeZone.UTC));
        }

        public abstract ViewEntity build();
    }
}
