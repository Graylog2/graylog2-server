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
package org.graylog2.rest.models.system.ldap.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
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


    @JsonCreator
    public static LdapSettingsResponse create(@JsonProperty("enabled") boolean enabled,
                                              @JsonProperty("system_username") String systemUsername,
                                              @JsonProperty("system_password") String systemPassword,
                                              @JsonProperty("ldap_uri") URI ldapUri,
                                              @JsonProperty("use_start_tls") boolean useStartTls,
                                              @JsonProperty("trust_all_certificates") boolean trustAllCertificates,
                                              @JsonProperty("active_directory") boolean activeDirectory,
                                              @JsonProperty("search_base") String searchBase,
                                              @JsonProperty("search_pattern") String searchPattern,
                                              @JsonProperty("display_name_attributes") String displayNameAttribute,
                                              @JsonProperty("default_group") String defaultGroup) {
        return new AutoValue_LdapSettingsResponse(enabled, systemUsername, systemPassword, ldapUri, useStartTls, trustAllCertificates, activeDirectory, searchBase, searchPattern, displayNameAttribute, defaultGroup);
    }
}
