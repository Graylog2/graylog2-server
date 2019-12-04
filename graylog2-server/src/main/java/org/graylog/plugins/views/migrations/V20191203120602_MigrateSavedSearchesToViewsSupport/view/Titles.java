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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class Titles {
    private static final String KEY_WIDGETS = "widget";
    private static final String KEY_QUERY = "tab";
    private static final String KEY_TITLE = "title";

    @JsonValue
    abstract Map<String, Map<String, String>> titles();

    public static Titles ofWidgetTitles(Map<String, String> titles) {
        return ofTitles(Collections.singletonMap(KEY_WIDGETS, titles));
    }

    static Titles ofTitles(Map<String, Map<String, String>> titles) {
        return new AutoValue_Titles(titles);
    }
}
