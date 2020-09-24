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

import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
public abstract class AuthServiceResult {
    public abstract String username();

    @Nullable
    public abstract String userProfileId();

    public abstract String backendId();

    public abstract String backendType();

    public abstract String backendTitle();

    public boolean isSuccess() {
        return !isNullOrEmpty(userProfileId());
    }

    public static Builder builder() {
        return new AutoValue_AuthServiceResult.Builder();
    }

    public static AuthServiceResult failed(String username, AuthServiceBackend backend) {
        return builder()
                .username(username)
                .backendId(backend.backendId())
                .backendType(backend.backendType())
                .backendTitle(backend.backendTitle())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder username(String username);

        public abstract Builder userProfileId(String userProfileId);

        public abstract Builder backendId(String backendId);

        public abstract Builder backendType(String backendType);

        public abstract Builder backendTitle(String backendTitle);

        public abstract AuthServiceResult build();
    }
}
