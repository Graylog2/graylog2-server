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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotBlank;

@AutoValue
@JsonDeserialize(builder = ViewClusterConfig.Builder.class)
@WithBeanGetter
public abstract class ViewClusterConfig {
    private static final String FIELD_DEFAULT_VIEW_ID = "default_view_id";

    @JsonProperty(FIELD_DEFAULT_VIEW_ID)
    @NotBlank
    public abstract String defaultViewId();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_DEFAULT_VIEW_ID)
        public abstract Builder defaultViewId(String defaultViewId);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ViewClusterConfig.Builder();
        }

        public abstract ViewClusterConfig build();
    }
}
