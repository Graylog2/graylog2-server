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
package org.graylog2.rest.resources.system.ldap;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.ldap.requests.LdapSettingsRequest;
import org.graylog2.rest.resources.system.ldap.requests.LdapTestConfigRequest;
import org.graylog2.rest.resources.system.ldap.responses.LdapSettingsResponse;
import org.graylog2.rest.resources.system.ldap.responses.LdapTestConfigResponse;
import org.graylog2.security.RestPermissions;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapEntry;
import org.graylog2.security.ldap.LdapSettings;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@RequiresAuthentication
@RequiresPermissions(RestPermissions.LDAP_EDIT)
// TODO even viewing the settings needs this permission, because it contains a password
@Api(value = "System/LDAP", description = "LDAP settings")
@Path("/system/ldap")
public class LdapResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(LdapResource.class);

    @Inject
    private LdapSettingsService ldapSettingsService;

    @Inject
    private LdapSettingsImpl.Factory ldapSettingsFactory;

    @Inject
    private LdapConnector ldapConnector;

    @Inject
    private LdapUserAuthenticator ldapAuthenticator;

    @GET
    @Timed
    @ApiOperation("Get the LDAP configuration if it is configured")
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public LdapSettingsResponse getLdapSettings() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        return LdapSettingsResponse.create(
                ldapSettings.isEnabled(),
                ldapSettings.getSystemUserName(),
                ldapSettings.getSystemPassword(),
                ldapSettings.getUri(),
                ldapSettings.isUseStartTls(),
                ldapSettings.isTrustAllCertificates(),
                ldapSettings.isActiveDirectory(),
                ldapSettings.getSearchBase(),
                ldapSettings.getSearchPattern(),
                ldapSettings.getDisplayNameAttribute(),
                ldapSettings.getDefaultGroup());
    }

    @POST
    @Timed
    @ApiOperation("Test LDAP Configuration")
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LdapTestConfigResponse testLdapConfiguration(@ApiParam(name = "Configuration to test", required = true)
                                                        @Valid @NotNull LdapTestConfigRequest request) {
        final LdapConnectionConfig config = new LdapConnectionConfig();
        final URI ldapUri = request.ldapUri();
        config.setLdapHost(ldapUri.getHost());
        config.setLdapPort(ldapUri.getPort());
        config.setUseSsl(ldapUri.getScheme().startsWith("ldaps"));
        config.setUseTls(request.useStartTls());

        if (request.trustAllCertificates()) {
            config.setTrustManagers(new TrustAllX509TrustManager());
        }

        if (!isNullOrEmpty(request.systemUsername()) && !isNullOrEmpty(request.systemPassword())) {
            config.setName(request.systemUsername());
            config.setCredentials(request.systemPassword());
        }

        LdapNetworkConnection connection = null;
        try {
            try {
                connection = ldapConnector.connect(config);
            } catch (LdapException e) {
                return LdapTestConfigResponse.create(false, false, false, Collections.<String, String>emptyMap(), e.getMessage());
            }

            if (null == connection) {
                return LdapTestConfigResponse.create(false, false, false, Collections.<String, String>emptyMap(), "Could not connect to LDAP server");
            }

            boolean connected = connection.isConnected();
            boolean systemAuthenticated = connection.isAuthenticated();

            // the web interface allows testing the connection only, in that case we can bail out early.
            if (request.testConnectOnly()) {
                return LdapTestConfigResponse.create(connected, systemAuthenticated, false, Collections.<String, String>emptyMap());
            }

            String userPrincipalName = null;
            boolean loginAuthenticated = false;
            Map<String, String> entryMap = Collections.emptyMap();
            String exception = null;
            try {
                final LdapEntry entry = ldapConnector.search(
                        connection,
                        request.searchBase(),
                        request.searchPattern(),
                        request.principal(),
                        request.activeDirectory());
                if (entry != null) {
                    userPrincipalName = entry.getDn();
                    entryMap = entry.getAttributes();
                }
            } catch (CursorException | LdapException e) {
                exception = e.getMessage();
            }

            try {
                loginAuthenticated = ldapConnector.authenticate(connection, userPrincipalName, request.password());
            } catch (Exception e) {
                exception = e.getMessage();
            }

            return LdapTestConfigResponse.create(connected, systemAuthenticated, loginAuthenticated, entryMap, exception);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException e) {
                    LOG.warn("Unable to close LDAP connection.", e);
                }
            }
        }
    }

    @PUT
    @Timed
    @ApiOperation("Update the LDAP configuration")
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateLdapSettings(@ApiParam(name = "JSON body", required = true)
                                   @Valid @NotNull LdapSettingsRequest request) throws ValidationException {
        // load the existing config, or create a new one. we only support having one, currently
        final LdapSettings ldapSettings = firstNonNull(ldapSettingsService.load(), ldapSettingsFactory.createEmpty());

        ldapSettings.setSystemUsername(request.systemUsername());
        ldapSettings.setSystemPassword(request.systemPassword());
        ldapSettings.setUri(request.ldapUri());
        ldapSettings.setUseStartTls(request.useStartTls());
        ldapSettings.setTrustAllCertificates(request.trustAllCertificates());
        ldapSettings.setActiveDirectory(request.activeDirectory());
        ldapSettings.setSearchPattern(request.searchPattern());
        ldapSettings.setSearchBase(request.searchBase());
        ldapSettings.setEnabled(request.enabled());
        ldapSettings.setDisplayNameAttribute(request.displayNameAttribute());
        ldapSettings.setDefaultGroup(request.defaultGroup());

        ldapSettingsService.save(ldapSettings);
        ldapAuthenticator.applySettings(ldapSettings);
    }

    @DELETE
    @Timed
    @ApiOperation("Remove the LDAP configuration")
    @Path("/settings")
    public void deleteLdapSettings() {
        ldapSettingsService.delete();
        ldapAuthenticator.applySettings(null);
    }
}
