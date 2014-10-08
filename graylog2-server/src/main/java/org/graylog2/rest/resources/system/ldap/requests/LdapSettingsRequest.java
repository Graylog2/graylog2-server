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
package org.graylog2.rest.resources.system.ldap.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.NotEmpty;

import java.net.URI;

@JsonAutoDetect
@AutoValue
public abstract class LdapSettingsRequest {
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
    public static LdapSettingsRequest create(@JsonProperty("enabled") boolean enabled,
                                             @JsonProperty("system_username") @NotEmpty String systemUsername,
                                             @JsonProperty("system_password") @NotEmpty String systemPassword,
                                             @JsonProperty("ldap_uri") URI ldapUri,
                                             @JsonProperty("use_start_tls") boolean useStartTls,
                                             @JsonProperty("trust_all_certificates") boolean trustAllCertificates,
                                             @JsonProperty("active_directory") boolean activeDirectory,
                                             @JsonProperty("search_base") @NotEmpty String searchBase,
                                             @JsonProperty("search_pattern") @NotEmpty String searchPattern,
                                             @JsonProperty("display_name_attribute") @NotEmpty String displayNameAttribute,
                                             @JsonProperty("default_group") @NotEmpty String defaultGroup) {
        return new AutoValue_LdapSettingsRequest(enabled, systemUsername, systemPassword, ldapUri, useStartTls, trustAllCertificates, activeDirectory, searchBase, searchPattern, displayNameAttribute, defaultGroup);
    }
}
