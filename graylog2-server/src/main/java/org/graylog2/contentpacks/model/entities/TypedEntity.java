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

public interface TypedEntity {
    String FIELD_META_TYPE = "type";

    @JsonProperty(FIELD_META_TYPE)
    ModelTypeEntity type();

    default String typeString() {
        return type().type().asString();
    }

    interface TypeBuilder<SELF> {
        @JsonProperty(FIELD_META_TYPE)
        SELF type(ModelTypeEntity type);
    }
}
