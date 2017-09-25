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
package org.graylog2.rest.models.dashboards.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class WidgetPositionsRequest {
    @JsonProperty
    public abstract List<WidgetPosition> positions();

    @JsonCreator
    public static WidgetPositionsRequest create(@JsonProperty("positions") @NotEmpty List<WidgetPosition> positions) {
        return new AutoValue_WidgetPositionsRequest(positions);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class WidgetPosition {
        @JsonProperty
        public abstract String id();

        @JsonProperty
        public abstract int col();

        @JsonProperty
        public abstract int row();

        @JsonProperty
        public abstract int height();

        @JsonProperty
        public abstract int width();

        @JsonCreator
        public static WidgetPosition create(@JsonProperty("id") @NotEmpty String id,
                                            @JsonProperty("col") @Min(0) int col,
                                            @JsonProperty("row") @Min(0) int row,
                                            @JsonProperty("height") @Min(0) int height,
                                            @JsonProperty("width") @Min(0) int width) {
            return new AutoValue_WidgetPositionsRequest_WidgetPosition(id, col, row, height, width);
        }
    }
}
