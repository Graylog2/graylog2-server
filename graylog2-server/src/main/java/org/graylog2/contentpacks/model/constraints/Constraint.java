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
package org.graylog2.contentpacks.model.constraints;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.Typed;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = Constraint.FIELD_META_TYPE)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GraylogVersionConstraint.class, name = GraylogVersionConstraint.TYPE),
        @JsonSubTypes.Type(value = PluginVersionConstraint.class, name = PluginVersionConstraint.TYPE)
})
public interface Constraint extends Typed {
    interface ConstraintBuilder<SELF> extends Typed.TypeBuilder<SELF> {
    }
}
