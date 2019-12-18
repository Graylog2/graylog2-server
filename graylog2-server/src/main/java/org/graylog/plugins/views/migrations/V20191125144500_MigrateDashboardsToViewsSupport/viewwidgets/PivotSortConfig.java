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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.PivotSort;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.SortSpec;

@AutoValue
public abstract class PivotSortConfig implements SortConfig {
    public static final String Type = "pivot";

    @Override
    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @Override
    @JsonProperty(FIELD_FIELD)
    public abstract String field();

    @Override
    @JsonProperty(FIELD_DIRECTION)
    public abstract Direction direction();

    @Override
    public SortSpec toSortSpec() {
        return PivotSort.create(field(),toDirection());
    }

    @JsonCreator
    public static PivotSortConfig create(@JsonProperty(FIELD_FIELD) String field,
                                         @JsonProperty(FIELD_DIRECTION) Direction direction) {
        return new AutoValue_PivotSortConfig(PivotSortConfig.Type, field, direction);
    }
}
