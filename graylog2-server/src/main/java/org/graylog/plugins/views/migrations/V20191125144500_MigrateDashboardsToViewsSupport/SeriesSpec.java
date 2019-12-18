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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class SeriesSpec {
    @JsonProperty
    public abstract String type();

    @JsonProperty
    @Nullable
    public abstract String id();

    @JsonProperty
    public abstract Optional<String> field();

    public String literal() {
        return type() + "(" + field().orElse("") + ")";
    }

    public static SeriesSpec create(String type, String id, String field) {
        return new AutoValue_SeriesSpec(type, id, Optional.ofNullable(field));
    }
}
