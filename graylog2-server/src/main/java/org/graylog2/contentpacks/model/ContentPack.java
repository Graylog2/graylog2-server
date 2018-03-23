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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = Versioned.FIELD_META_VERSION, defaultImpl = LegacyContentPack.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = LegacyContentPack.class),
        @JsonSubTypes.Type(value = ContentPackV1.class, name = ContentPackV1.VERSION)
})
public interface ContentPack extends Identified, Revisioned, Versioned {
    interface ContentPackBuilder<SELF> extends IdBuilder<SELF>, RevisionBuilder<SELF>, VersionBuilder<SELF> {
    }
}
