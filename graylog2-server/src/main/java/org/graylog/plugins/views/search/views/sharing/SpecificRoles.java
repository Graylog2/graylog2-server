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
package org.graylog.plugins.views.search.views.sharing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonTypeName(SpecificRoles.TYPE)
public abstract class SpecificRoles implements ViewSharing {
    private static final String FIELD_ROLES = "roles";
    public static final String TYPE = "roles";

    @JsonProperty
    @Override
    public abstract String type();

    @JsonProperty(FIELD_VIEW_ID)
    @Override
    public abstract String viewId();

    @JsonProperty(FIELD_ROLES)
    public abstract Set<String> roles();

    @JsonCreator
    public static SpecificRoles create(@JsonProperty(FIELD_VIEW_ID) String viewId, @JsonProperty(FIELD_ROLES) Set<String> roles) {
        return new AutoValue_SpecificRoles(TYPE, viewId, roles);
    }
}
