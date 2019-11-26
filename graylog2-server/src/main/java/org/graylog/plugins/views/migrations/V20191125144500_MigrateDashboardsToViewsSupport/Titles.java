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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@AutoValue
abstract class Titles {
    private static final String KEY_WIDGETS = "widget";

    @JsonValue
    abstract Map<String, Map<String, String>> titles();

    @JsonCreator
    static Titles of(Map<String, Map<String, String>> titles) {
        return new AutoValue_Titles(titles);
    }

    static Titles empty() {
        return of(Collections.emptyMap());
    }

    Optional<String> widgetTitle(String widgetId) {
        return Optional.ofNullable(titles().getOrDefault(KEY_WIDGETS, Collections.emptyMap()).get(widgetId));
    }
}
