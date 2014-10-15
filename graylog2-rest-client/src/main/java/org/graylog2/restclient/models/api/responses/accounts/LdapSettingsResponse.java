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
package org.graylog2.restclient.models.api.responses.accounts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

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
}
