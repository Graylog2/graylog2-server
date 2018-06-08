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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class CollectorAction {

    @JsonProperty("collector_id")
    public abstract String collectorId();

    @JsonProperty("properties")
    public abstract Map<String, Object> properties();

    @JsonCreator
    public static CollectorAction create(@JsonProperty("collector_id") String collectorId,
                                         @JsonProperty("properties") Map<String, Object> properties) {
        return new AutoValue_CollectorAction(collectorId, properties);
    }

    public static CollectorAction create(String collectorId, String action) {
        return create(collectorId, ImmutableMap.of(action, true));
    }
}
