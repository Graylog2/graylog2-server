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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Pivot {
    private static String TYPE_TIME = "time";
    private static String TYPE_VALUES = "values";

    static final String FIELD_FIELD_NAME = "field";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";

    @JsonProperty(FIELD_FIELD_NAME)
    public abstract String field();

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    public abstract PivotConfig config();

    public static Builder timeBuilder() {
        return new AutoValue_Pivot.Builder()
                .type(TYPE_TIME);
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder field(String field);

        public abstract Builder config(PivotConfig config);

        public abstract Builder type(String type);

        public abstract Pivot build();
    }
}
