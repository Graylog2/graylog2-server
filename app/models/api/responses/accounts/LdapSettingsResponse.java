/*
 * Copyright 2013 TORCH UG
 *
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
package models.api.responses.accounts;

import com.google.gson.annotations.SerializedName;

import java.net.URI;

public class LdapSettingsResponse {
    @SerializedName("enabled")
    private boolean enabled;

    @SerializedName("system_username")
    private String systemUsername;

    @SerializedName("system_password")
    private String systemPassword;

    @SerializedName("ldap_uri")
    private URI ldapUri;

    @SerializedName("search_base")
    private String searchBase;

    @SerializedName("principal_search_pattern")
    private String principalSearchPattern;

    @SerializedName("username_attribute")
    private String usernameAttribute;

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

    public String getPrincipalSearchPattern() {
        return principalSearchPattern;
    }

    public void setPrincipalSearchPattern(String principalSearchPattern) {
        this.principalSearchPattern = principalSearchPattern;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }
}
