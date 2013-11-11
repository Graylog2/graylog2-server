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
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.apache.shiro.realm.ldap.JndiLdapContextFactory;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.documentation.annotations.ApiParam;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.requests.LdapSettingsRequest;
import org.graylog2.rest.resources.system.requests.LdapTestLoginRequest;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
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
        result.put("principal_search_pattern", ldapSettings.getPrincipalSearchPattern());
        result.put("username_attribute", ldapSettings.getUsernameAttribute());
        return ok(json(result)).build();
    }

    @POST
    @Timed
    @ApiOperation("")
    @Path("/testconnection")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testLdapConnection(@ApiParam(title = "JSON body", required = true) String body) {
        LdapSettingsRequest request;
        try {
            request = objectMapper.readValue(body, LdapSettingsRequest.class);
        } catch (IOException e) {
            log.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        JndiLdapContextFactory contextFactory = new JndiLdapContextFactory();
        contextFactory.setUrl(request.ldapUri.toString());
        contextFactory.setSystemUsername(request.systemUsername);
        contextFactory.setSystemPassword(request.systemPassword);

        Map<String, Object> result = Maps.newHashMap();
        try {
            final LdapConnector ldapConnector = core.getLdapRealm().ldapConnector;
            final LdapContext context = ldapConnector.connect(request.ldapUri,
                                                              request.systemUsername,
                                                              request.systemPassword);
            context.close();
            result.put("successful", true);
        } catch (NamingException e) {
            result.put("successful", false);
            result.put("exception", e.toString());
        }

        return ok(json(result)).build();
    }

    @POST
    @Timed
    @ApiOperation("")
    @Path("/testlogin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response testLdapLogin(@ApiParam(title = "JSON body", required = true) String body) {
        LdapTestLoginRequest request;
        try {
            request = objectMapper.readValue(body, LdapTestLoginRequest.class);
        } catch (IOException e) {
            log.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Map<String, Object> result = Maps.newHashMap();
        try {
            final LdapConnector ldapConnector = core.getLdapRealm().ldapConnector;
            final Map<String, String> entry = ldapConnector.loadAccount(request.ldapUri,
                                                                        request.systemUsername,
                                                                        request.systemPassword,
                                                                        request.searchBase,
                                                                        request.principalSearchPattern,
                                                                        request.testUsername);
            final Map<String, String> entryWithoutPassword = Maps.filterKeys(entry, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    return !"userPassword".equals(input);
                }
            });
            result.put("successful", true);
            result.put("attributes", entryWithoutPassword);
        } catch (NamingException e) {
            result.put("successful", false);
            result.put("exception", e.toString());
        }
        return ok(json(result)).build();
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
        ldapSettings.setPrincipalSearchPattern(request.principalSearchPattern);
        ldapSettings.setSearchBase(request.searchBase);
        ldapSettings.setEnabled(request.isEnabled);
        ldapSettings.setUsernameAttribute(request.usernameAttribute);

        try {
            ldapSettings.save();
        } catch (ValidationException e) {
            log.error("Invalid LDAP settings, not updated!", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        core.getLdapRealm().applySettings(ldapSettings);

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
