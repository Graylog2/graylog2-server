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
package org.graylog2.restclient.models.api.requests.accounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.api.requests.ApiRequest;

public class LdapTestConnectionRequest extends ApiRequest {

    @JsonProperty("active_directory")
    public boolean isActiveDirectory;

    @JsonProperty("ldap_uri")
    public String ldapUri;

    @JsonProperty("system_username")
    public String systemUsername;

    @JsonProperty("system_password")
    public String systemPassword;

    @JsonProperty("use_start_tls")
    public boolean useStartTls;

    @JsonProperty("trust_all_certificates")
    public boolean trustAllCertificates;

    @JsonProperty("test_connect_only")
    public boolean testConnectOnly;

    @JsonProperty("search_base")
    public String searchBase;

    @JsonProperty("search_pattern")
    public String searchPattern;

    @JsonProperty("principal")
    public String principal;

    @JsonProperty("password")
    public String password;
}
