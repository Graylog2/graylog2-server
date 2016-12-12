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
package org.graylog2.rest.models.system.ldap.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.net.URI;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class LdapTestConfigRequest {

    @JsonProperty
    @Nullable
    public abstract String systemUsername();

    @JsonProperty
    @Nullable
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
    @Nullable
    public abstract String searchBase();

    @JsonProperty
    @Nullable
    public abstract String searchPattern();

    @JsonProperty
    @Nullable
    public abstract String principal();

    @JsonProperty
    @Nullable
    public abstract String password();

    @JsonProperty
    public abstract boolean testConnectOnly();

    @JsonProperty
    @Nullable
    public abstract String groupSearchBase();

    @JsonProperty
    @Nullable
    public abstract String groupIdAttribute();

    @JsonProperty
    @Nullable
    public abstract String groupSearchPattern();

    @JsonCreator
    public static LdapTestConfigRequest create(@JsonProperty("system_username") @Nullable String systemUsername,
                                               @JsonProperty("system_password") @Nullable String systemPassword,
                                               @JsonProperty("ldap_uri") URI ldapUri,
                                               @JsonProperty("use_start_tls") boolean useStartTls,
                                               @JsonProperty("trust_all_certificates") boolean trustAllCertificates,
                                               @JsonProperty("active_directory") boolean activeDirectory,
                                               @JsonProperty("search_base") @Nullable String searchBase,
                                               @JsonProperty("search_pattern") @Nullable String searchPattern,
                                               @JsonProperty("principal") @Nullable String principal,
                                               @JsonProperty("password") @Nullable String password,
                                               @JsonProperty("test_connect_only") boolean testConnectOnly,
                                               @JsonProperty("group_search_base") @Nullable String groupSearchBase,
                                               @JsonProperty("group_id_attribute") @Nullable String groupIdAttribute,
                                               @JsonProperty("group_search_pattern") @Nullable String groupSearchPattern) {
        return new AutoValue_LdapTestConfigRequest(systemUsername,
                                                   systemPassword,
                                                   ldapUri,
                                                   useStartTls,
                                                   trustAllCertificates,
                                                   activeDirectory,
                                                   searchBase,
                                                   searchPattern,
                                                   principal,
                                                   password,
                                                   testConnectOnly,
                                                   groupSearchBase,
                                                   groupIdAttribute,
                                                   groupSearchPattern);
    }
}
