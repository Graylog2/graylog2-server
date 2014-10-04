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
package org.graylog2.restclient.models.accounts;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.accounts.LdapSettingsRequest;
import org.graylog2.restclient.models.api.responses.accounts.LdapSettingsResponse;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.net.URI;

public class LdapSettings {
    private static final Logger log = LoggerFactory.getLogger(LdapSettings.class);

    private final ApiClient api;
    private final LdapSettingsResponse response;

    @AssistedInject
    private LdapSettings(ApiClient api, @Assisted LdapSettingsResponse response) {
        this.api = api;
        this.response = response;
    }

    @AssistedInject
    private LdapSettings(ApiClient api, @Assisted LdapSettingsRequest request) {
        this.api = api;
        final LdapSettingsResponse response = new LdapSettingsResponse();
        response.setEnabled(request.enabled);
        response.setSystemUsername(request.systemUsername);
        response.setSystemPassword(request.systemPassword);
        response.setLdapUri(URI.create(request.ldapUri));
        response.setSearchPattern(request.searchPattern);
        response.setSearchBase(request.searchBase);
        response.setDisplayNameAttribute(request.displayNameAttribute);
        response.setActiveDirectory(request.activeDirectory);
        response.setUseStartTls(request.useStartTls);
        response.setTrustAllCertificates(request.trustAllCertificates);
        response.setDefaultGroup(request.defaultGroup);
        this.response = response;
    }

    public boolean save() {
        LdapSettingsRequest request = toRequest();

        try {
            api.path(routes.LdapResource().updateLdapSettings()).body(request).expect(Http.Status.NO_CONTENT).execute();
            return true;
        } catch (APIException e) {
            log.error("Unable to save LDAP settings.", e);
            return false;
        } catch (IOException e) {
            log.error("Unable to save LDAP settings.", e);
            return false;
        }
    }

    public LdapSettingsRequest toRequest() {
        LdapSettingsRequest request = new LdapSettingsRequest();
        request.enabled = isEnabled();
        request.systemUsername = getSystemUsername();
        request.systemPassword = getSystemPassword();
        request.ldapUri = getLdapUri().toString();
        request.searchPattern = getSearchPattern();
        request.searchBase = getSearchBase();
        request.displayNameAttribute = getDisplayNameAttribute();
        request.activeDirectory = isActiveDirectory();
        request.useStartTls = isUseStartTls();
        request.trustAllCertificates = isTrustAllCertificates();
        request.defaultGroup = getDefaultGroup();
        return request;
    }

    public boolean delete() {
        try {
            api.path(routes.LdapResource().deleteLdapSettings()).expect(Http.Status.NO_CONTENT).execute();
            return true;
        } catch (APIException e) {
            log.error("Unable to remove LDAP settings", e);
            return false;
        } catch (IOException e) {
            log.error("Unable to remove LDAP settings", e);
            return false;
        }
    }

    public interface Factory {
        LdapSettings fromSettingsResponse(LdapSettingsResponse response);
        LdapSettings fromSettingsRequest(LdapSettingsRequest request);
    }

    public boolean isEnabled() {
        return response.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        response.setEnabled(enabled);
    }


    public String getSystemPassword() {
        return response.getSystemPassword();
    }

    public void setSearchPattern(String searchPattern) {
        response.setSearchPattern(searchPattern);
    }

    public String getDisplayNameAttribute() {
        return response.getDisplayNameAttribute();
    }

    public void setDisplayUsername(String systemUsername) {
        response.setSystemUsername(systemUsername);
    }

    public void setSearchBase(String searchBase) {
        response.setSearchBase(searchBase);
    }

    public String getSearchPattern() {
        return response.getSearchPattern();
    }

    public String getSystemUsername() {
        return response.getSystemUsername();
    }

    public void setSystemPassword(String systemPassword) {
        response.setSystemPassword(systemPassword);
    }

    public String getSearchBase() {
        return response.getSearchBase();
    }

    public URI getLdapUri() {
        return response.getLdapUri();
    }

    public void setLdapUri(URI ldapUri) {
        response.setLdapUri(ldapUri);
    }

    public void setUseStartTls(boolean useStartTls) {
        response.setUseStartTls(useStartTls);
    }

    public boolean isUseStartTls() {
        return response.isUseStartTls();
    }

    public boolean isTrustAllCertificates() {
        return response.isTrustAllCertificates();
    }

    public void setTrustAllCertificates(boolean trustAllCertificates) {
        response.setTrustAllCertificates(trustAllCertificates);
    }

    public void setActiveDirectory(boolean activeDirectory) {
        response.setActiveDirectory(activeDirectory);
    }

    public boolean isActiveDirectory() {
        return response.isActiveDirectory();
    }

    public String getDefaultGroup() {
        return response.getDefaultGroup();
    }

    public void setDefaultGroup(String defaultGroup) {
        response.setDefaultGroup(defaultGroup);
    }
}
