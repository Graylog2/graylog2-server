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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class StreamRuleEntity {
    @JsonProperty("type")
    @NotNull
    public abstract ValueReference type();

    @JsonProperty("field")
    @NotBlank
    public abstract ValueReference field();

    @JsonProperty("value")
    @NotNull
    public abstract ValueReference value();

    @JsonProperty("inverted")
    public abstract ValueReference inverted();

    @JsonProperty("description")
    public abstract ValueReference description();

    @JsonCreator
    public static StreamRuleEntity create(
            @JsonProperty("type") @NotNull ValueReference type,
            @JsonProperty("field") @NotBlank ValueReference field,
            @JsonProperty("value") @NotNull ValueReference value,
            @JsonProperty("inverted") ValueReference inverted,
            @JsonProperty("description") ValueReference description) {
        return new AutoValue_StreamRuleEntity(type, field, value, inverted, description);
    }
}
