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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AutoValue
public abstract class Titles {
    private static final String KEY_WIDGETS = "widget";

    @JsonValue
    public abstract Map<String, Map<String, String>> titles();

    @JsonCreator
    public static Titles of(Map<String, Map<String, String>> titles) {
        return new AutoValue_Titles(titles);
    }

    public static Titles withWidgetTitle(Map<String, String> widgetTitleMap) {
        final Map<String, Map<String, String>> titlesMap = new HashMap<>(1);
        titlesMap.put(KEY_WIDGETS, widgetTitleMap);
        return of(titlesMap);
    }

    public static Titles empty() {
        return of(Collections.emptyMap());
    }

    public Optional<String> widgetTitle(String widgetId) {
        return Optional.ofNullable(titles().getOrDefault(KEY_WIDGETS, Collections.emptyMap()).get(widgetId));
    }
}
