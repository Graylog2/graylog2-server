/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.ldap;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.system.ldap.requests.LdapSettingsRequest;
import org.graylog2.rest.models.system.ldap.requests.LdapTestConfigRequest;
import org.graylog2.rest.models.system.ldap.responses.LdapSettingsResponse;
import org.graylog2.rest.models.system.ldap.responses.LdapTestConfigResponse;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.shiro.authz.annotation.Logical.OR;

@RequiresAuthentication

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

    @GET
    @Timed
    @RequiresPermissions(RestPermissions.LDAP_EDIT)
    @ApiOperation("Get the LDAP configuration if it is configured")
    @Path("/settings")
    @Produces(MediaType.APPLICATION_JSON)
    public LdapSettingsResponse getLdapSettings() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings == null) {
            return LdapSettingsResponse.emptyDisabled();
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
                ldapSettings.getDefaultGroup(),
                ldapSettings.getGroupMapping(),
                ldapSettings.getGroupSearchBase(),
                ldapSettings.getGroupIdAttribute(),
                ldapSettings.getAdditionalDefaultGroups(),
                ldapSettings.getGroupSearchPattern());
    }

    @POST
    @Timed
    @RequiresPermissions(RestPermissions.LDAP_EDIT)
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
                return LdapTestConfigResponse.create(false, false, false, Collections.<String, String>emptyMap(), Collections.<String>emptySet(), e.getMessage());
            }

            if (null == connection) {
                return LdapTestConfigResponse.create(false, false, false, Collections.<String, String>emptyMap(), Collections.<String>emptySet(), "Could not connect to LDAP server");
            }

            boolean connected = connection.isConnected();
            boolean systemAuthenticated = connection.isAuthenticated();

            // the web interface allows testing the connection only, in that case we can bail out early.
            if (request.testConnectOnly()) {
                return LdapTestConfigResponse.create(connected, systemAuthenticated, false, Collections.<String, String>emptyMap(), Collections.<String>emptySet());
            }

            String userPrincipalName = null;
            boolean loginAuthenticated = false;
            Map<String, String> entryMap = Collections.emptyMap();
            String exception = null;
            Set<String> groups = Collections.emptySet();
            try {
                final LdapEntry entry = ldapConnector.search(
                        connection,
                        request.searchBase(),
                        request.searchPattern(),
                        "*",
                        request.principal(),
                        request.activeDirectory(),
                        request.groupSearchBase(),
                        request.groupIdAttribute(),
                        request.groupSearchPattern());
                if (entry != null) {
                    userPrincipalName = entry.getBindPrincipal();
                    entryMap = entry.getAttributes();
                    groups = entry.getGroups();
                }
            } catch (CursorException | LdapException e) {
                exception = e.getMessage();
            }

            try {
                loginAuthenticated = ldapConnector.authenticate(connection, userPrincipalName, request.password());
            } catch (Exception e) {
                exception = e.getMessage();
            }

            return LdapTestConfigResponse.create(connected, systemAuthenticated, loginAuthenticated, entryMap, groups, exception);
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
    @RequiresPermissions(RestPermissions.LDAP_EDIT)
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
        ldapSettings.setGroupMapping(request.groupMapping());
        ldapSettings.setGroupSearchBase(request.groupSearchBase());
        ldapSettings.setGroupIdAttribute(request.groupIdAttribute());
        ldapSettings.setGroupSearchPattern(request.groupSearchPattern());
        ldapSettings.setAdditionalDefaultGroups(request.additionalDefaultGroups());

        ldapSettingsService.save(ldapSettings);
    }

    @DELETE
    @Timed
    @RequiresPermissions(RestPermissions.LDAP_EDIT)
    @ApiOperation("Remove the LDAP configuration")
    @Path("/settings")
    public void deleteLdapSettings() {
        ldapSettingsService.delete();
    }

    @GET
    @ApiOperation(value = "Get the LDAP group to Graylog role mapping", notes = "The return value is a simple hash with keys being the LDAP group names and the values the corresponding Graylog role names.")
    @RequiresPermissions(value = {RestPermissions.LDAPGROUPS_READ, RestPermissions.LDAP_EDIT}, logical = OR)
    @Path("/settings/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> readGroupMapping(){
        final LdapSettings ldapSettings = firstNonNull(ldapSettingsService.load(), ldapSettingsFactory.createEmpty());
        return ldapSettings.getGroupMapping();
    }

    @PUT
    @RequiresPermissions(value = {RestPermissions.LDAPGROUPS_EDIT, RestPermissions.LDAP_EDIT}, logical = OR)
    @ApiOperation(value = "Update the LDAP group to Graylog role mapping", notes = "Corresponds directly to the output of GET /system/ldap/settings/groups")
    @Path("/settings/groups")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateGroupMappingSettings(@ApiParam(name = "JSON body", required = true, value = "A hash in which the keys are the LDAP group names and values is the Graylog role name.")
                                   @NotNull Map<String, String> groupMapping) throws ValidationException {
        final LdapSettings ldapSettings = firstNonNull(ldapSettingsService.load(), ldapSettingsFactory.createEmpty());

        ldapSettings.setGroupMapping(groupMapping);
        ldapSettingsService.save(ldapSettings);

        return Response.noContent().build();
    }

    @GET
    @ApiOperation(value = "Get the available LDAP groups", notes = "")
    @RequiresPermissions(RestPermissions.LDAPGROUPS_READ)
    @Path("/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> readGroups(){
        final LdapSettings ldapSettings = firstNonNull(ldapSettingsService.load(), ldapSettingsFactory.createEmpty());

        if (!ldapSettings.isEnabled()) {
            throw new BadRequestException("LDAP is disabled.");
        }
        if (Strings.isNullOrEmpty(ldapSettings.getGroupSearchBase()) || Strings.isNullOrEmpty(ldapSettings.getGroupIdAttribute())) {
            throw new BadRequestException("LDAP group configuration settings are not set.");
        }

        final LdapConnectionConfig config = new LdapConnectionConfig();
        final URI ldapUri = ldapSettings.getUri();
        config.setLdapHost(ldapUri.getHost());
        config.setLdapPort(ldapUri.getPort());
        config.setUseSsl(ldapUri.getScheme().startsWith("ldaps"));
        config.setUseTls(ldapSettings.isUseStartTls());

        if (ldapSettings.isTrustAllCertificates()) {
            config.setTrustManagers(new TrustAllX509TrustManager());
        }

        if (!isNullOrEmpty(ldapSettings.getSystemUserName()) && !isNullOrEmpty(ldapSettings.getSystemPassword())) {
            config.setName(ldapSettings.getSystemUserName());
            config.setCredentials(ldapSettings.getSystemPassword());
        }

        try (LdapNetworkConnection connection = ldapConnector.connect(config)) {
            return ldapConnector.listGroups(connection,
                                            ldapSettings.getGroupSearchBase(),
                                            ldapSettings.getGroupSearchPattern(),
                                            ldapSettings.getGroupIdAttribute()
            );
        } catch (IOException | LdapException e) {
            LOG.error("Unable to retrieve available LDAP groups", e);
            throw new InternalServerErrorException("Unable to retrieve available LDAP groups", e);
        }
    }
}
