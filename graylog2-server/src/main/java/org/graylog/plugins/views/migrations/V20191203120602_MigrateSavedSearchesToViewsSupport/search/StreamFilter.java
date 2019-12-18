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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;

@AutoValue
@JsonAutoDetect
abstract class StreamFilter {
    abstract String streamId();

    @JsonValue
    public Map<String, Object> value() {
        return ImmutableMap.of(
                "type", "or",
                "filters", ImmutableSet.of(
                        ImmutableMap.of(
                                "type", "stream",
                                "id", streamId()
                                )
                )
        );
    }

    public static StreamFilter create(String streamId) {
        return new AutoValue_StreamFilter(streamId);
    }
}
