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
public abstract class UnboundLDAPConfig {
    public abstract String userSearchBase();

    public abstract String userSearchPattern();

    public abstract String userUniqueIdAttribute();

    public abstract String userNameAttribute();

    public abstract String userFullNameAttribute();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public static Builder create() {
            return new AutoValue_UnboundLDAPConfig.Builder();
        }

        public abstract Builder userSearchBase(String userSearchBase);

        public abstract Builder userSearchPattern(String userSearchPattern);

        public abstract Builder userUniqueIdAttribute(String userUniqueIdAttribute);

        public abstract Builder userNameAttribute(String userNameAttribute);

        public abstract Builder userFullNameAttribute(String userFullNameAttribute);

        public abstract UnboundLDAPConfig build();
    }
}
