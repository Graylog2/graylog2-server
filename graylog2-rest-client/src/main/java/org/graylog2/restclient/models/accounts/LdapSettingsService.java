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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.accounts.LdapSettingsRequest;
import org.graylog2.restclient.models.api.requests.accounts.LdapTestConnectionRequest;
import org.graylog2.restclient.models.api.responses.accounts.LdapConnectionTestResponse;
import org.graylog2.restclient.models.api.responses.accounts.LdapSettingsResponse;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class LdapSettingsService {
    private static final Logger log = LoggerFactory.getLogger(LdapSettingsService.class);

    @Inject
    private ApiClient api;

    @Inject
    private LdapSettings.Factory ldapSettingsFactory;

    public LdapSettings load() {
        LdapSettingsResponse response;
        try {
            response = api.path(routes.LdapResource().getLdapSettings(), LdapSettingsResponse.class).execute();
        } catch (APIException e) {
            log.error("Unable to load LDAP settings.", e);
            return null;
        } catch (IOException e) {
            log.error("Unable to load LDAP settings.", e);
            return null;
        }
        final LdapSettings ldapSettings = ldapSettingsFactory.fromSettingsResponse(response);
        return ldapSettings;
    }

    public LdapSettings create(LdapSettingsRequest request) {
        if (!request.enabled) {
            // the other fields will be "disabled" in the form, thus all values will be null.
            // load the old settings, and set "enabled" to false in the response.
            final LdapSettings ldapSettings = load();
            ldapSettings.setEnabled(request.enabled);
            return ldapSettings;
        }
        // otherwise just create the new settings object.
        return ldapSettingsFactory.fromSettingsRequest(request);
    }

    public LdapConnectionTestResponse testLdapConfiguration(LdapTestConnectionRequest request) throws APIException, IOException {
        return api.path(routes.LdapResource().testLdapConfiguration(), LdapConnectionTestResponse.class).body(request).execute();
    }
}
