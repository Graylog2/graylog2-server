/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
