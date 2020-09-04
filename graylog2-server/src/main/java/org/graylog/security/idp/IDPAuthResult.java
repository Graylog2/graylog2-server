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
package org.graylog.security.idp;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
public abstract class IDPAuthResult {
    public abstract String username();

    @Nullable
    public abstract String userProfileId();

    public abstract String providerId();

    public abstract String providerTitle();

    public boolean isSuccess() {
        return !isNullOrEmpty(userProfileId());
    }

    public static Builder builder() {
        return new AutoValue_IDPAuthResult.Builder();
    }

    public static IDPAuthResult failed(String username, String providerId, String providerTitle) {
        return builder()
                .username(username)
                .providerId(providerId)
                .providerTitle(providerTitle)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder username(String username);

        public abstract Builder userProfileId(String userProfileId);

        public abstract Builder providerId(String providerId);

        public abstract Builder providerTitle(String providerTitle);

        public abstract IDPAuthResult build();
    }
}
