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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.Typed;

@AutoValue
@JsonDeserialize(builder = AutoValue_EntityExcerpt.Builder.class)
public abstract class EntityExcerpt implements Identified, Typed {
    public static final String FIELD_TITLE = "title";

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    public static Builder builder() {
        return new AutoValue_EntityExcerpt.Builder();
    }

    @AutoValue.Builder
    public interface Builder extends IdBuilder<Builder>, TypeBuilder<Builder> {
        @JsonProperty(FIELD_TITLE)
        Builder title(String title);

        EntityExcerpt build();
    }
}
