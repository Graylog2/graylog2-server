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

import java.net.URI;

@JsonAutoDetect
@AutoValue
public abstract class LdapSettingsResponse {
    @JsonProperty
    public abstract boolean enabled();

    @JsonProperty
    public abstract String systemUsername();

    @JsonProperty
    public abstract String systemPassword();

    @JsonProperty
    public abstract URI ldapUri();

    @JsonProperty
    public abstract boolean useStartTls();

    @JsonProperty
    public abstract boolean trustAllCertificates();

    @JsonProperty
    public abstract boolean activeDirectory();

    @JsonProperty
    public abstract String searchBase();

    @JsonProperty
    public abstract String searchPattern();

    @JsonProperty
    public abstract String displayNameAttribute();

    @JsonProperty
    public abstract String defaultGroup();


    public static LdapSettingsResponse create(boolean enabled,
                                              String systemUsername,
                                              String systemPassword,
                                              URI ldapUri,
                                              boolean useStartTls,
                                              boolean trustAllCertificates,
                                              boolean activeDirectory,
                                              String searchBase,
                                              String searchPattern,
                                              String displayNameAttribute,
                                              String defaultGroup) {
        return new AutoValue_LdapSettingsResponse(enabled, systemUsername, systemPassword, ldapUri, useStartTls, trustAllCertificates, activeDirectory, searchBase, searchPattern, displayNameAttribute, defaultGroup);
    }
}
