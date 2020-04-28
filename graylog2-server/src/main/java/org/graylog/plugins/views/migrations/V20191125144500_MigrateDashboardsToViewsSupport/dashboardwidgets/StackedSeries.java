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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class StackedSeries {
    public abstract String query();
    public abstract String field();
    public abstract String statisticalFunction();

    @JsonCreator
    public static StackedSeries create(
            @JsonProperty("query") @Nullable String query,
            @JsonProperty("field") String field,
            @JsonProperty("statistical_function") String statisticalFunction
    ) {
        final String cleanedQuery = Strings.nullToEmpty(query).trim();
        return new AutoValue_StackedSeries(cleanedQuery.equals("") ? "*" : cleanedQuery, field, statisticalFunction);
    }
}
