/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.dashboards.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class WidgetPositions {
    @JsonProperty
    public abstract List<WidgetPosition> positions();

    @JsonCreator
    public static WidgetPositions create(@JsonProperty("positions") @NotEmpty List<WidgetPosition> positions) {
        return new AutoValue_WidgetPositions(positions);
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class WidgetPosition {
        @JsonProperty
        public abstract String id();

        @JsonProperty
        public abstract int col();

        @JsonProperty
        public abstract int row();

        @JsonCreator
        public static WidgetPosition create(@JsonProperty("id") @NotEmpty String id,
                                            @JsonProperty("col") @Min(0) int col,
                                            @JsonProperty("row") @Min(0) int row) {
            return new AutoValue_WidgetPositions_WidgetPosition(id, col, row);
        }
    }
}
