/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.rest.resources.system.inputs.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class InputLaunchRequest {
    @JsonProperty
    public abstract String title();

    @JsonProperty
    public abstract String type();

    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty
    public abstract boolean global();

    @JsonProperty
    public abstract Map<String, Object> configuration();

    @JsonCreator
    public static InputLaunchRequest create(@JsonProperty("title") String title,
                                            @JsonProperty("type") String type,
                                            @JsonProperty("creator_user_id") String creatorUserId,
                                            @JsonProperty("global") boolean global,
                                            @JsonProperty("configuration") Map<String, Object> configuration) {
        return new AutoValue_InputLaunchRequest(title, type, creatorUserId, global, configuration);
    }
}