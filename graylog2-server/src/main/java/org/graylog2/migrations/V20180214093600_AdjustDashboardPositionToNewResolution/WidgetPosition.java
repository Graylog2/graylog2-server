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
package org.graylog2.migrations.V20180214093600_AdjustDashboardPositionToNewResolution;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = WidgetPosition.Builder.class)
public abstract class WidgetPosition {

    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("width")
    public abstract int width();

    @JsonProperty("height")
    public abstract int height();

    @JsonProperty("col")
    public abstract int col();

    @JsonProperty("row")
    public abstract int row();

    public static Builder builder() {
        return new AutoValue_WidgetPosition.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract WidgetPosition build();

        public abstract Builder id(String id);

        public abstract Builder width(int width);

        public abstract Builder height(int height);

        public abstract Builder col(int col);

        public abstract Builder row(int row);
    }
}
