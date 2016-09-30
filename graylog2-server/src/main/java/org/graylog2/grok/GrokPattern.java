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
package org.graylog2.grok;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class GrokPattern {

    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract String pattern();

    @JsonProperty
    @Nullable
    public abstract String contentPack();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .add("name", name())
                .add("pattern", pattern())
                .add("contentPack", contentPack())
                .toString();
    }

    @JsonCreator
    public static GrokPattern create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                     @JsonProperty("name") String name,
                                     @JsonProperty("pattern") String pattern,
                                     @JsonProperty("contentPack") @Nullable String contentPack) {
        return builder()
                .id(id)
                .name(name)
                .pattern(pattern)
                .contentPack(contentPack)
                .build();
    }

    public static GrokPattern create(String name, String pattern) {
        return create(null, name, pattern, null);
    }

    public static Builder builder() {
        return new AutoValue_GrokPattern.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder name(String name);

        public abstract Builder pattern(String pattern);

        public abstract Builder contentPack(String contentPack);

        public abstract GrokPattern build();
    }
}
