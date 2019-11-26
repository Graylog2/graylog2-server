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
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@JsonDeserialize(builder = ViewWidgetPosition.Builder.class)
@WithBeanGetter
abstract class ViewWidgetPosition {
    @JsonProperty("col")
    abstract Position col();

    @JsonProperty("row")
    abstract Position row();

    @JsonProperty("height")
    abstract Position height();

    @JsonProperty("width")
    abstract Position width();

    @AutoValue.Builder
    static abstract class Builder {
        @JsonProperty("col")
        abstract Builder col(Position col);

        @JsonProperty("row")
        abstract Builder row(Position row);

        @JsonProperty("height")
        abstract Builder height(Position height);

        @JsonProperty("width")
        abstract Builder width(Position width);

        abstract ViewWidgetPosition build();

        @JsonCreator
        static Builder create() { return new AutoValue_ViewWidgetPosition.Builder(); }
    }
}
