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
