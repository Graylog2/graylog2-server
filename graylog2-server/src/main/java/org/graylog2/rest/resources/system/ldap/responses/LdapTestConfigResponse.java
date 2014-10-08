/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.ldap.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;

@JsonAutoDetect
@AutoValue
public abstract class LdapTestConfigResponse {
    @JsonProperty
    public abstract boolean connected();

    @JsonProperty
    public abstract boolean systemAuthenticated();

    @JsonProperty
    public abstract boolean loginAuthenticated();

    @JsonProperty
    public abstract Map<String, String> entry();

    @JsonProperty
    @Nullable
    public abstract String exception();

    public static LdapTestConfigResponse create(boolean connected,
                                                boolean systemAuthenticated,
                                                boolean loginAuthenticated,
                                                Map<String, String> entry,
                                                @Nullable String exception) {
        return new AutoValue_LdapTestConfigResponse(connected, systemAuthenticated, loginAuthenticated, entry, exception);
    }
    public static LdapTestConfigResponse create(boolean connected,
                                                boolean systemAuthenticated,
                                                boolean loginAuthenticated,
                                                Map<String, String> entry) {
        return new AutoValue_LdapTestConfigResponse(connected, systemAuthenticated, loginAuthenticated, entry, null);
    }
}
