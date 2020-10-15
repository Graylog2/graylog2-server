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
package org.graylog.security.authservice;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
public abstract class UserDetails {
    public abstract Optional<String> databaseId();

    public abstract String authServiceType();

    public abstract String authServiceId();

    public abstract String base64AuthServiceUid();

    public abstract String username();

    public abstract String email();

    public abstract String fullName();

    public abstract Set<String> defaultRoles();

    public UserDetails withDatabaseId(String id) {
        checkArgument(!isNullOrEmpty(id), "id cannot be null or empty");

        return toBuilder().databaseId(id).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_UserDetails.Builder();
        }

        public abstract Builder databaseId(@Nullable String databaseId);

        public abstract Builder authServiceType(String authServiceType);

        public abstract Builder authServiceId(String authServiceId);

        public abstract Builder base64AuthServiceUid(String base64AuthServiceUid);

        public abstract Builder username(String username);

        public abstract Builder email(String email);

        public abstract Builder fullName(String fullName);

        public abstract Builder defaultRoles(Set<String> defaultRoles);

        public abstract UserDetails build();
    }
}
