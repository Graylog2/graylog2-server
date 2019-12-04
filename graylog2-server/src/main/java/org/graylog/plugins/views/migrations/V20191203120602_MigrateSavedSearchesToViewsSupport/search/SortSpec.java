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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface SortSpec {
    enum Direction {
        Ascending,
        Descending
    }

    String TYPE_FIELD = "type";
    String FIELD_FIELD = "field";
    String FIELD_DIRECTION = "direction";

    @JsonProperty(TYPE_FIELD)
    String type();
    @JsonProperty(FIELD_FIELD)
    String field();
    @JsonProperty(FIELD_DIRECTION)
    Direction direction();
}
