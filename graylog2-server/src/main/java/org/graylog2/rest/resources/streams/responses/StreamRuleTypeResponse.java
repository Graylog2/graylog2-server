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
package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamRuleTypeResponse {
    @JsonProperty
    public abstract int id();

    @JsonProperty
    public abstract String name();

    @JsonProperty("short_desc")
    public abstract String shortDesc();

    @JsonProperty("long_desc")
    public abstract String longDesc();

    @JsonCreator
    public static StreamRuleTypeResponse create(@JsonProperty("id") int id,
                                                @JsonProperty("name") String name,
                                                @JsonProperty("short_desc") String shortDesc,
                                                @JsonProperty("long_desc") String longDesc) {
        return new AutoValue_StreamRuleTypeResponse(id, name, shortDesc, longDesc);
    }
}
