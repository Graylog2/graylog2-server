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
package org.graylog.security;

import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
public abstract class RoleDTO {
    public abstract String id();

    public abstract String title();

    public abstract Set<String> permissions();

    public static RoleDTO create(String id, String title, Set<String> permissions) {
        return builder()
                .id(id)
                .title(title)
                .permissions(permissions)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RoleDTO.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder permissions(Set<String> permissions);

        public abstract RoleDTO build();
    }
}
