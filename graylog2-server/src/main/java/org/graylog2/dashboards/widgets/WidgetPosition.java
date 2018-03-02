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
package org.graylog2.dashboards.widgets;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class WidgetPosition {

    public abstract String id();
    public abstract Integer width();
    public abstract Integer height();
    public abstract Integer col();
    public abstract Integer row();

    public static WidgetPosition create(String id, Integer width, Integer height, Integer col, Integer row) {
        return new AutoValue_WidgetPosition(id, width, height, col, row);
    }
}
