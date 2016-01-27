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
package org.graylog2.rest.resources.roles;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.rest.models.roles.responses.RoleMembershipResponse;
import org.graylog2.rest.models.roles.responses.RoleResponse;
import org.graylog2.rest.models.roles.responses.RolesResponse;
import org.graylog2.rest.models.users.responses.UserSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.graylog2.users.RoleImpl;
import org.graylog2.users.RoleService;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static javax.ws.rs.core.Response.status;

@RequiresAuthentication
@Path("/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Roles", description = "User roles")
public class RolesResource extends RestResource {
    private static final Logger log = LoggerFactory.getLogger(RolesResource.class);

    private final RoleService roleService;

    @Inject
    public RolesResource(RoleService roleService) {
        this.roleService = roleService;
    }

    @GET
    @RequiresPermissions(RestPermissions.ROLES_READ)
    @ApiOperation(value = "List all roles", notes = "")
    public RolesResponse listAll() throws NotFoundException {
        final Set<Role> roles = roleService.loadAll();
        Set<RoleResponse> roleResponses = Sets.newHashSet();
        for (Role role : roles) {
            roleResponses.add(RoleResponse.create(role.getName(), Optional.fromNullable(role.getDescription()), role.getPermissions(), role.isReadOnly()));
        }

        return RolesResponse.create(roleResponses);
    }

    @GET
    @Path("{rolename}")
    @ApiOperation("Retrieve permissions for a single role")
    public RoleResponse read(@ApiParam(name = "rolename", required = true) @PathParam("rolename") String name) throws NotFoundException {
        checkPermission(RestPermissions.ROLES_READ, name);

        final Role role = roleService.load(name);
        return RoleResponse.create(role.getName(), Optional.fromNullable(role.getDescription()), role.getPermissions(), role.isReadOnly());
    }

    @POST
    @RequiresPermissions(RestPermissions.ROLES_CREATE)
    @ApiOperation(value = "Create a new role", notes = "")
    public Response create(@ApiParam(name = "JSON body", value = "The new role to create", required = true) @Valid @NotNull RoleResponse roleResponse) {
        if (roleService.exists(roleResponse.name())) {
            throw new BadRequestException("Role " + roleResponse.name() + " already exists.");
        }

        Role role = new RoleImpl();
        role.setName(roleResponse.name());
        role.setPermissions(roleResponse.permissions());
        role.setDescription(roleResponse.description().orNull());

        try {
            role = roleService.save(role);
        } catch (ValidationException e) {
            log.error("Invalid role creation request.");
            throw new BadRequestException(e);
        }

        final URI uri = getUriBuilderToSelf().path(RolesResource.class)
                .path("{rolename}")
                .build(role.getName());

        return Response.created(uri).entity(RoleResponse.create(role.getName(),
                                                                Optional.fromNullable(role.getDescription()),
                                                                role.getPermissions(),
                                                                role.isReadOnly())).build();
    }

    @PUT
    @Path("{rolename}")
    @ApiOperation("Update an existing role")
    public RoleResponse update(
            @ApiParam(name = "rolename", required = true) @PathParam("rolename") String name,
            @ApiParam(name = "JSON Body", value = "The new representation of the role", required = true) RoleResponse role) throws NotFoundException {
        final Role roleToUpdate = roleService.load(name);

        if (roleToUpdate.isReadOnly()) {
            throw new BadRequestException("Cannot update read only role " + name);
        }
        roleToUpdate.setName(role.name());
        roleToUpdate.setDescription(role.description().orNull());
        roleToUpdate.setPermissions(role.permissions());
        try {
            roleService.save(roleToUpdate);
        } catch (ValidationException e) {
            throw new BadRequestException(e);
        }
        return RoleResponse.create(roleToUpdate.getName(), Optional.fromNullable(roleToUpdate.getDescription()), roleToUpdate.getPermissions(),
                                   role.readOnly());
    }

    @DELETE
    @Path("{rolename}")
    @ApiOperation(value = "Remove the named role and dissociate any users from it")
    public void delete(@ApiParam(name = "rolename", required = true) @PathParam("rolename") String name) throws NotFoundException {
        checkPermission(RestPermissions.ROLES_DELETE, name);

        final Role role = roleService.load(name);
        if (role.isReadOnly()) {
            throw new BadRequestException("Cannot delete read only system role " + name);
        }
        userService.dissociateAllUsersFromRole(role);

        if (roleService.delete(name) == 0) {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("{rolename}/members")
    @RequiresPermissions({RestPermissions.USERS_LIST, RestPermissions.ROLES_READ})
    @ApiOperation(value = "Retrieve the role's members")
    public RoleMembershipResponse getMembers(@ApiParam(name = "rolename", required = true) @PathParam("rolename") String name) throws NotFoundException {
        final Role role = roleService.load(name);
        final Collection<User> users = userService.loadAllForRole(role);

        Set<UserSummary> userSummaries = Sets.newHashSetWithExpectedSize(users.size());
        for (User user : users) {
            final Set<String> roleNames = userService.getRoleNames(user);

            userSummaries.add(UserSummary.create(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getFullName(),
                    isPermitted(RestPermissions.USERS_PERMISSIONSEDIT,
                                user.getName()) ? userService.getPermissionsForUser(user) : Collections.<String>emptyList(),
                    user.getPreferences(),
                    firstNonNull(user.getTimeZone(), DateTimeZone.UTC).getID(),
                    user.getSessionTimeoutMs(),
                    user.isReadOnly(),
                    user.isExternalUser(),
                    user.getStartpage(),
                    roleNames
            ));
        }

        return RoleMembershipResponse.create(role.getName(), userSummaries);
    }

    @PUT
    @Path("{rolename}/members/{username}")
    @ApiOperation("Add a user to a role")
    public Response addMember(@ApiParam(name = "rolename") @PathParam("rolename") String rolename,
                              @ApiParam(name = "username") @PathParam("username") String username,
                              @ApiParam(name = "JSON Body", value = "Placeholder because PUT requests should have a body. Set to '{}', the content will be ignored.") String body) throws NotFoundException {
        checkPermission(RestPermissions.ROLES_EDIT, username);

        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException("User " + username + " has not been found.");
        }

        // verify that the role exists
        final Role role = roleService.load(rolename);

        final HashSet<String> roles = Sets.newHashSet(user.getRoleIds());
        roles.add(role.getId());
        user.setRoleIds(roles);

        try {
            userService.save(user);
        } catch (ValidationException e) {
            throw new BadRequestException("Validation failed", e);
        }

        return status(Response.Status.NO_CONTENT).build();
    }

    @DELETE
    @Path("{rolename}/members/{username}")
    @ApiOperation("Remove a user from a role")
    public Response removeMember(@ApiParam(name = "rolename") @PathParam("rolename") String rolename,
                                 @ApiParam(name = "username") @PathParam("username") String username) throws NotFoundException {
        checkPermission(RestPermissions.ROLES_EDIT, username);

        final User user = userService.load(username);
        if (user == null) {
            throw new NotFoundException("User " + username + " has not been found.");
        }

        // verify that the role exists
        final Role role = roleService.load(rolename);

        final HashSet<String> roles = Sets.newHashSet(user.getRoleIds());
        roles.remove(role.getId());
        user.setRoleIds(roles);

        try {
            userService.save(user);
        } catch (ValidationException e) {
            throw new BadRequestException("Validation failed", e);
        }


        return status(Response.Status.NO_CONTENT).build();
    }
}
