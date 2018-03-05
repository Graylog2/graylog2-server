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
    public abstract int width();
    public abstract int height();
    public abstract int col();
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
