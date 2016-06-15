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
package org.graylog2.decorators;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.hibernate.validator.constraints.NotBlank;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
@CollectionName("decorators")
public abstract class DecoratorImpl implements Decorator {
    static final String FIELD_ID = "_id";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIG = "config";
    static final String FIELD_STREAM = "stream";

    @JsonProperty(FIELD_ID)
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    @NotBlank
    @Override
    public abstract String type();

    @JsonProperty(FIELD_CONFIG)
    @Override
    public abstract Map<String, Object> config();

    @JsonProperty(FIELD_STREAM)
    @Override
    public abstract Optional<String> stream();

    @JsonCreator
    public static DecoratorImpl create(@JsonProperty(FIELD_ID) @Nullable String id,
                                       @JsonProperty(FIELD_TYPE) String type,
                                       @JsonProperty(FIELD_CONFIG) Map<String, Object> config,
                                       @JsonProperty(FIELD_STREAM) Optional<String> stream) {
        return new AutoValue_DecoratorImpl(id, type, config, stream);
    }

    public static Decorator create(@JsonProperty(FIELD_TYPE) String type,
                                   @JsonProperty(FIELD_CONFIG) Map<String, Object> config,
                                   @JsonProperty(FIELD_STREAM) Optional<String> stream) {
        return create(null, type, config, stream);
    }

    public static Decorator create(@JsonProperty(FIELD_TYPE) String type,
                                   @JsonProperty(FIELD_CONFIG) Map<String, Object> config) {
        return create(type, config, Optional.empty());
    }
}
