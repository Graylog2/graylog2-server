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
package org.graylog.security.authservice.ldap;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class LDAPUser {
    public abstract String base64UniqueId();

    public abstract boolean accountIsEnabled();

    public abstract String username();

    public abstract String fullName();

    public abstract String email();

    public abstract LDAPEntry entry();

    public String dn() {
        return entry().dn();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_LDAPUser.Builder();
        }

        public abstract Builder base64UniqueId(String base64UniqueId);

        public abstract Builder accountIsEnabled(boolean isEnabled);

        public abstract Builder username(String username);

        public abstract Builder fullName(String fullName);

        public abstract Builder email(String email);

        public abstract Builder entry(LDAPEntry entry);

        public abstract LDAPUser build();
    }
}
