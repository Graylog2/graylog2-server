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
import com.google.common.collect.Maps;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.ValidationException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.ldap.requests.LdapSettingsRequest;
import org.graylog2.rest.resources.system.ldap.requests.LdapTestConfigRequest;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;

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
    public Response getLdapSettings() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings == null) {
            return noContent().build();
        }
        Map<String, Object> result = Maps.newHashMap();
        result.put("enabled", ldapSettings.isEnabled());
        result.put("system_username", ldapSettings.getSystemUserName());
        result.put("system_password", ldapSettings.getSystemPassword()); // TODO AES encrypt
        result.put("ldap_uri", ldapSettings.getUri());
        result.put("search_base", ldapSettings.getSearchBase());
        result.put("search_pattern", ldapSettings.getSearchPattern());
        result.put("display_name_attribute", ldapSettings.getDisplayNameAttribute());
        result.put("active_directory", ldapSettings.isActiveDirectory());
        result.put("use_start_tls", ldapSettings.isUseStartTls());
        result.put("trust_all_certificates", ldapSettings.isTrustAllCertificates());
        result.put("default_group", ldapSettings.getDefaultGroup());

        return ok(json(result)).build();
    }

    @POST
    @Timed
    @ApiOperation("Test LDAP Configuration")
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LdapTestConfigResponse testLdapConfiguration(@ApiParam(name = "Configuration to test", required = true) LdapTestConfigRequest request) {
        LdapTestConfigResponse response = new LdapTestConfigResponse();

        final LdapConnectionConfig config = new LdapConnectionConfig();
        final URI ldapUri = request.ldapUri;
        config.setLdapHost(ldapUri.getHost());
        config.setLdapPort(ldapUri.getPort());
        config.setUseSsl(ldapUri.getScheme().startsWith("ldaps"));
        config.setUseTls(request.useStartTls);
        if (request.trustAllCertificates) {
            config.setTrustManagers(new TrustAllX509TrustManager());
        }
        if (request.systemUsername != null && !request.systemUsername.isEmpty()) {
            config.setName(request.systemUsername);
            config.setCredentials(request.systemPassword);
        }

        LdapNetworkConnection connection = null;
        try {
            try {
                connection = ldapConnector.connect(config);
            } catch (LdapException e) {
                response.exception = e.getMessage();
                response.connected = false;
                response.systemAuthenticated = false;
                return response;
            }

            if (null == connection) {
                response.connected = false;
                response.systemAuthenticated = false;
                response.exception = "Could not connect to LDAP server";
                return response;
            }

            response.connected = connection.isConnected();
            response.systemAuthenticated = connection.isAuthenticated();

            // the web interface allows testing the connection only, in that case we can bail out early.
            if (request.testConnectOnly) {
                return response;
            }

            String userPrincipalName = null;
            try {
                final LdapEntry entry = ldapConnector.search(
                        connection,
                        request.searchBase,
                        request.searchPattern,
                        request.principal,
                        request.activeDirectory);
                if (entry != null) {
                    userPrincipalName = entry.getDn();
                    response.entry = entry.getAttributes();
                }
            } catch (CursorException | LdapException e) {
                response.entry = null;
                response.exception = e.getMessage();
            }

            try {
                response.loginAuthenticated = ldapConnector.authenticate(connection, userPrincipalName, request.password);
            } catch (Exception e) {
                response.loginAuthenticated = false;
                response.exception = e.getMessage();
            }

            return response;
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
    public void updateLdapSettings(@ApiParam(name = "JSON body", required = true) String body) {
        LdapSettingsRequest request;
        try {
            request = objectMapper.readValue(body, LdapSettingsRequest.class);
        } catch (IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        // load the existing config, or create a new one. we only support having one, currently
        LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings == null) {
            ldapSettings = ldapSettingsFactory.createEmpty();
        }
        ldapSettings.setSystemUsername(request.systemUsername);
        ldapSettings.setSystemPassword(request.systemPassword);
        ldapSettings.setUri(request.ldapUri);
        ldapSettings.setUseStartTls(request.useStartTls);
        ldapSettings.setTrustAllCertificates(request.trustAllCertificates);
        ldapSettings.setActiveDirectory(request.activeDirectory);
        ldapSettings.setSearchPattern(request.searchPattern);
        ldapSettings.setSearchBase(request.searchBase);
        ldapSettings.setEnabled(request.enabled);
        ldapSettings.setDisplayNameAttribute(request.displayNameAttribute);
        ldapSettings.setDefaultGroup(request.defaultGroup);

        try {
            ldapSettingsService.save(ldapSettings);
        } catch (ValidationException e) {
            LOG.error("Invalid LDAP settings, not updated!", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
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
