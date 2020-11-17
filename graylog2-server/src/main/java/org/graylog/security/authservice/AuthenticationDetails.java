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

import java.util.Collections;
import java.util.Map;

@AutoValue
public abstract class AuthenticationDetails {

    public abstract UserDetails userDetails();

    public abstract Map<String, Object> sessionAttributes();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public static Builder create() {
            return new AutoValue_AuthenticationDetails.Builder()
                    .sessionAttributes(Collections.emptyMap());
        }

        public abstract Builder userDetails(UserDetails userDetails);

        public abstract Builder sessionAttributes(Map<String, Object> sessionData);

        public abstract AuthenticationDetails build();
    }
}
