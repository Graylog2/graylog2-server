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
package models.api.requests.accounts;

import com.google.gson.annotations.SerializedName;
import models.api.requests.ApiRequest;

public class LdapSettingsRequest extends ApiRequest {

    @SerializedName("enabled")
    public boolean enabled;

    @SerializedName("active_directory")
    public boolean activeDirectory;

    @SerializedName("ldap_uri")
    public String ldapUri;

    @SerializedName("system_username")
    public String systemUsername;

    @SerializedName("system_password")
    public String systemPassword;

    @SerializedName("use_start_tls")
    public boolean useStartTls;

    @SerializedName("trust_all_certificates")
    public boolean trustAllCertificates;

    @SerializedName("search_base")
    public String searchBase;

    @SerializedName("search_pattern")
    public String searchPattern;

    @SerializedName("display_name_attribute")
    public String displayNameAttribute;

    @SerializedName("default_group")
    public String defaultGroup;
}
