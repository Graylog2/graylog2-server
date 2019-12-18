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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = DisplayModeSettings.Builder.class)
abstract class DisplayModeSettings {
    private static final String FIELD_POSITIONS = "positions";

    @JsonProperty(FIELD_POSITIONS)
    abstract Map<String, ViewWidgetPosition> positions();

    static DisplayModeSettings empty() {
        return Builder.create().build();
    }

    @AutoValue.Builder
    static abstract class Builder {
        @JsonProperty(FIELD_POSITIONS)
        abstract Builder positions(Map<String, ViewWidgetPosition> positions);

        abstract DisplayModeSettings build();

        @JsonCreator
        static Builder create() {
            return new AutoValue_DisplayModeSettings.Builder()
                    .positions(Collections.emptyMap());
        }
    }
}
