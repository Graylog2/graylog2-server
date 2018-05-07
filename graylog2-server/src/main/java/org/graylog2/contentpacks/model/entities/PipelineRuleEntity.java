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

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class PipelineRuleEntity {
    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    @Nullable
    public abstract String description();

    @JsonProperty("source")
    public abstract String source();

    @JsonCreator
    public static PipelineRuleEntity create(@JsonProperty("title") String title,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("source") String source) {
        return new AutoValue_PipelineRuleEntity(title, description, source);
    }
}
