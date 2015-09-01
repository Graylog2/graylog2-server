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
package org.graylog2.restclient.models.api.responses.accounts;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public class LdapSettingsResponse {
    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("system_username")
    private String systemUsername;

    @JsonProperty("system_password")
    private String systemPassword;

    @JsonProperty("ldap_uri")
    private URI ldapUri;

    @JsonProperty("search_base")
    private String searchBase;

    @JsonProperty("search_pattern")
    private String searchPattern;

    @JsonProperty("display_name_attribute")
    private String displayNameAttribute;

    @JsonProperty("active_directory")
    private boolean activeDirectory;

    @JsonProperty("use_start_tls")
    private boolean useStartTls;

    @JsonProperty("trust_all_certificates")
    private boolean trustAllCertificates;

    @JsonProperty("default_group")
    private String defaultGroup;

    @JsonProperty("group_mapping")
    @Nullable
    public Map<String, String> groupMapping;

    @JsonProperty("group_search_base")
    @Nullable
    public String groupSearchBase;

    @JsonProperty("group_id_attribute")
    @Nullable
    public String groupIdAttribute;

    @JsonProperty("group_search_pattern")
    @Nullable
    public String groupSearchPattern;

    @JsonProperty("additional_default_groups")
    @Nullable
    public Set<String> additionalDefaultGroups;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSystemUsername() {
        return systemUsername;
    }

    public void setSystemUsername(String systemUsername) {
        this.systemUsername = systemUsername;
    }

    public String getSystemPassword() {
        return systemPassword;
    }

    public void setSystemPassword(String systemPassword) {
        this.systemPassword = systemPassword;
    }

    public URI getLdapUri() {
        return ldapUri;
    }

    public void setLdapUri(URI ldapUri) {
        this.ldapUri = ldapUri;
    }

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    public String getDisplayNameAttribute() {
        return displayNameAttribute;
    }

    public void setDisplayNameAttribute(String displayNameAttribute) {
        this.displayNameAttribute = displayNameAttribute;
    }

    public void setActiveDirectory(boolean activeDirectory) {
        this.activeDirectory = activeDirectory;
    }

    public boolean isActiveDirectory() {
        return activeDirectory;
    }

    public void setUseStartTls(boolean useStartTls) {
        this.useStartTls = useStartTls;
    }

    public boolean isUseStartTls() {
        return useStartTls;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public boolean isTrustAllCertificates() {
        return trustAllCertificates;
    }

    public void setTrustAllCertificates(boolean trustAllCertificates) {
        this.trustAllCertificates = trustAllCertificates;
    }

    @Nullable
    public Map<String, String> getGroupMapping() {
        return groupMapping;
    }

    public void setGroupMapping(@Nullable Map<String, String> groupMapping) {
        this.groupMapping = groupMapping;
    }

    @Nullable
    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(@Nullable String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    @Nullable
    public String getGroupIdAttribute() {
        return groupIdAttribute;
    }

    public void setGroupIdAttribute(@Nullable String groupIdAttribute) {
        this.groupIdAttribute = groupIdAttribute;
    }

    @Nullable
    public String getGroupSearchPattern() {
        return groupSearchPattern;
    }

    public void setGroupSearchPattern(@Nullable String groupSearchPattern) {
        this.groupSearchPattern = groupSearchPattern;
    }

    @Nullable
    public Set<String> getAdditionalDefaultGroups() {
        return additionalDefaultGroups;
    }

    public void setAdditionalDefaultGroups(@Nullable Set<String> additionalDefaultGroups) {
        this.additionalDefaultGroups = additionalDefaultGroups;
    }
}
