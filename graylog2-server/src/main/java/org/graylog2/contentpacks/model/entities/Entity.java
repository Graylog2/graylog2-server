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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog2.contentpacks.model.Identified;
import org.graylog2.contentpacks.model.Typed;
import org.graylog2.contentpacks.model.Versioned;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = Versioned.FIELD_META_VERSION)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EntityV1.class, name = EntityV1.VERSION)
})
public interface Entity extends Identified, Typed, Versioned {
    EntityDescriptor toEntityDescriptor();

    interface EntityBuilder<SELF> extends IdBuilder<SELF>, TypeBuilder<SELF>, VersionBuilder<SELF> {
    }
}
