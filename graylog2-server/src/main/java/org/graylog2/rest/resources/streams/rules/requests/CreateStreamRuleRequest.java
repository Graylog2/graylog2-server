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
package org.graylog2.rest.resources.streams.rules.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class CreateStreamRuleRequest {
    @JsonProperty
    public abstract int type();

    @JsonProperty
    public abstract String value();

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract boolean inverted();

    public static CreateStreamRuleRequest create(@JsonProperty("type") int type,
                                                 @JsonProperty("value") String value,
                                                 @JsonProperty("field") String field,
                                                 @JsonProperty("inverted") boolean inverted) {
        return new AutoValue_CreateStreamRuleRequest(type, value, field, inverted);
    }
}
