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
import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class LdapSettingsRequest {
    @JsonProperty
    public abstract boolean enabled();

    @JsonProperty
    public abstract String systemUsername();

    @JsonProperty
    @Nullable
    public abstract String systemPassword();

    @JsonProperty
    public abstract boolean isSystemPasswordSet();

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

    @JsonProperty
    @Nullable
    public abstract Map<String, String> groupMapping();

    @JsonProperty
    @Nullable
    public abstract String groupSearchBase();

    @JsonProperty
    @Nullable
    public abstract String groupIdAttribute();

    @JsonProperty
    @Nullable
    public abstract Set<String> additionalDefaultGroups();

    @JsonProperty
    @Nullable
    public abstract String groupSearchPattern();

    @JsonCreator
    public static LdapSettingsRequest create(@JsonProperty("enabled") boolean enabled,
                                             @JsonProperty("system_username") @NotEmpty String systemUsername,
                                             @JsonProperty("system_password") @Nullable String systemPassword,
                                             @JsonProperty("system_password_set") boolean isSystemPasswordSet,
                                             @JsonProperty("ldap_uri") URI ldapUri,
                                             @JsonProperty("use_start_tls") boolean useStartTls,
                                             @JsonProperty("trust_all_certificates") boolean trustAllCertificates,
                                             @JsonProperty("active_directory") boolean activeDirectory,
                                             @JsonProperty("search_base") @NotEmpty String searchBase,
                                             @JsonProperty("search_pattern") @NotEmpty String searchPattern,
                                             @JsonProperty("display_name_attribute") @NotEmpty String displayNameAttribute,
                                             @JsonProperty("default_group") @NotEmpty String defaultGroup,
                                             @JsonProperty("group_mapping") @Nullable Map<String, String> groupMapping,
                                             @JsonProperty("group_search_base") @Nullable String groupSearchBase,
                                             @JsonProperty("group_id_attribute") @Nullable String groupIdAttribute,
                                             @JsonProperty("additional_default_groups") @Nullable Set<String> additionalDefaultGroups,
                                             @JsonProperty("group_search_pattern") @Nullable String groupSearchPattern) {
        return new AutoValue_LdapSettingsRequest(enabled,
                                                 systemUsername,
                                                 systemPassword,
                                                 isSystemPasswordSet,
                                                 ldapUri,
                                                 useStartTls,
                                                 trustAllCertificates,
                                                 activeDirectory,
                                                 searchBase,
                                                 searchPattern,
                                                 displayNameAttribute,
                                                 defaultGroup,
                                                 groupMapping,
                                                 groupSearchBase,
                                                 groupIdAttribute,
                                                 additionalDefaultGroups,
                                                 groupSearchPattern);
    }
}
