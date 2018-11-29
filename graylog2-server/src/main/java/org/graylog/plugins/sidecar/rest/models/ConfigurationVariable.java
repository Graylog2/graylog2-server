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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Locale;

@AutoValue
public abstract class ConfigurationVariable {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CONTENT = "content";

    public static final String VARIABLE_PREFIX = "user";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String description();

    @JsonProperty(FIELD_CONTENT)
    public abstract String content();

    @JsonCreator
    public static ConfigurationVariable create(@JsonProperty(FIELD_ID) String id,
                                               @JsonProperty(FIELD_NAME) String name,
                                               @JsonProperty(FIELD_DESCRIPTION) String description,
                                               @JsonProperty(FIELD_CONTENT) String content) {
        return new AutoValue_ConfigurationVariable(id, name, description, content);
    }

    public static ConfigurationVariable create(String name, String description, String content) {
        return create(new org.bson.types.ObjectId().toHexString(),
                name,
                description,
                content);
    }

    @JsonIgnore
    public String fullName() {
       return String.format(Locale.ENGLISH, "${%s.%s}", VARIABLE_PREFIX, name());
    }
}
