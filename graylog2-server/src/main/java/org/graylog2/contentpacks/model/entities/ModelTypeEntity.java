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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

@AutoValue
public abstract class ModelTypeEntity {
    @JsonValue
    public abstract ValueReference type();

    @Override
    public String toString() {
        return type().asString();
    }

    @JsonCreator
    public static ModelTypeEntity of(ValueReference type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(type.asString()), "Type must not be blank");
        return new AutoValue_ModelTypeEntity(type);
    }

    public static ModelTypeEntity of(String type) {
        return of(ValueReference.of(type));
    }
}
