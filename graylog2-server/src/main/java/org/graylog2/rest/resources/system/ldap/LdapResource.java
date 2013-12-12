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
package org.graylog2.rest.resources.system.ldap;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.ldap.requests.LdapSettingsRequest;
import org.graylog2.rest.resources.system.ldap.requests.LdapTestConfigRequest;
import org.graylog2.rest.resources.system.ldap.responses.LdapTestConfigResponse;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapEntry;
import org.graylog2.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;

@Api(value = "System/LDAP", description = "LDAP settings")
@Path("/system/ldap")
public class LdapResource extends RestResource {

    private static final Logger log = LoggerFactory.getLogger(LdapResource.class);

    @GET
    @Timed
    @ApiOperation("Get the LDAP configuration if it is configured")
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLdapSettings() {
        final LdapSettings ldapSettings = LdapSettings.load(core);
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

        return ok(json(result)).build();
    }

    @POST
    @Timed
    @ApiOperation("Test LDAP Configuration")
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LdapTestConfigResponse testLdapConfiguration(@ApiParam(title = "Configuration to test", required = true) LdapTestConfigRequest request) {
        LdapTestConfigResponse response = new LdapTestConfigResponse();


        final LdapConnector ldapConnector = core.getLdapConnector();

        final LdapConnectionConfig config = new LdapConnectionConfig();
        final URI ldapUri = request.ldapUri;
        config.setLdapHost(ldapUri.getHost());
        config.setLdapPort(ldapUri.getPort());
        config.setUseSsl(ldapUri.getScheme().startsWith("ldaps"));
        config.setUseTls(request.useStartTls);
        // TODO this accepts every and all certificates, which is certainly wrong!
        config.setTrustManagers(new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        });
        if (request.systemUsername != null && !request.systemUsername.isEmpty()) {
            config.setName(request.systemUsername);
            config.setCredentials(request.systemPassword);
        }

        final LdapNetworkConnection connection;
        try {
            connection = ldapConnector.connect(config);
        } catch (LdapException e) {
            response.exception = e.getMessage();
            response.connected = false;
            response.systemAuthenticated = false;
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
        } catch (LdapException e) {
            response.entry = null;
            response.exception = e.getMessage();
        } catch (CursorException e) {
            response.entry = null;
            response.exception = e.getMessage();
        }
        try {
            response.loginAuthenticated = ldapConnector.authenticate(connection,
                                                                     userPrincipalName, request.password);
        } catch (Exception e) {
            response.loginAuthenticated = false;
            response.exception = e.getMessage();
        }

        return response;
    }

    @PUT
    @Timed
    @ApiOperation("Update the LDAP configuration")
    @Path("/settings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateLdapSettings(@ApiParam(title = "JSON body", required = true) String body) {
        LdapSettingsRequest request;
        try {
            request = objectMapper.readValue(body, LdapSettingsRequest.class);
        } catch (IOException e) {
            log.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        // load the existing config, or create a new one. we only support having one, currently
        LdapSettings ldapSettings = LdapSettings.load(core);
        if (ldapSettings == null) {
            ldapSettings = new LdapSettings(core);
        }
        ldapSettings.setSystemUsername(request.systemUsername);
        ldapSettings.setSystemPassword(request.systemPassword);
        ldapSettings.setUri(request.ldapUri);
        ldapSettings.setUseStartTls(request.useStartTls);
        ldapSettings.setActiveDirectory(request.activeDirectory);
        ldapSettings.setSearchPattern(request.searchPattern);
        ldapSettings.setSearchBase(request.searchBase);
        ldapSettings.setEnabled(request.enabled);
        ldapSettings.setDisplayNameAttribute(request.displayNameAttribute);

        try {
            ldapSettings.save();
        } catch (ValidationException e) {
            log.error("Invalid LDAP settings, not updated!", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        core.getLdapAuthenticator().applySettings(ldapSettings);

        return noContent().build();
    }

    @DELETE
    @Timed
    @ApiOperation("Remove the LDAP configuration")
    @Path("/settings")
    public Response deleteLdapSettings() {
        LdapSettings.delete(core);
        return noContent().build();
    }
}
