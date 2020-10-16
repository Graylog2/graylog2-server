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
import org.graylog2.security.encryption.EncryptedValue;

@AutoValue
public abstract class AuthServiceCredentials {
    public abstract String username();

    public abstract EncryptedValue password();

    public abstract boolean isAuthenticated();

    public static AuthServiceCredentials create(String username, EncryptedValue password) {
        return builder()
                .username(username)
                .password(password)
                .isAuthenticated(false)
                .build();
    }

    public static AuthServiceCredentials createAuthenticated(String username) {
        return builder()
                .username(username)
                .password(EncryptedValue.createUnset())
                .isAuthenticated(true)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_AuthServiceCredentials.Builder()
                    .isAuthenticated(false);
        }

        public abstract Builder username(String username);

        public abstract Builder password(EncryptedValue password);

        public abstract Builder isAuthenticated(boolean isAuthenticated);

        public abstract AuthServiceCredentials build();
    }
}
