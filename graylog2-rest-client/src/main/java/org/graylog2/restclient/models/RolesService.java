package org.graylog2.restclient.models;

import org.graylog2.rest.models.roles.responses.RoleMembershipResponse;
import org.graylog2.rest.models.roles.responses.RoleResponse;
import org.graylog2.rest.models.roles.responses.RolesResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class RolesService {
    private static final Logger log = LoggerFactory.getLogger(RolesService.class);

    private final ApiClient api;

    @Inject
    public RolesService(ApiClient api) {
        this.api = api;
    }

    public Set<RoleResponse> loadAll() {
        try {
            final RolesResponse rolesResponse = api.path(routes.RolesResource().listAll(),
                                                         RolesResponse.class).execute();
            return rolesResponse.roles();
        } catch (APIException | IOException e) {
            log.error("Unable to load roles list", e);
        }
        return Collections.emptySet();
    }

    public RoleResponse load(String roleName) {
        try {
            return api.path(routes.RolesResource().read(roleName), RoleResponse.class).execute();
        } catch (APIException | IOException e) {
            log.error("Unable to read role " + roleName, e);
        }
        return null;
    }

    public RoleResponse create(RoleResponse newRole) {
        try {
            return api.path(routes.RolesResource().create(), RoleResponse.class).body(newRole).execute();
        } catch (APIException | IOException e) {
            log.error("Unable to create role " + newRole.name(), e);
            return null;
        }
    }

    public RoleMembershipResponse getMembers(String roleName) {
        try {
            return api.path(routes.RolesResource().getMembers(roleName), RoleMembershipResponse.class).execute();
        } catch (APIException | IOException e) {
            log.error("Unable to retrieve membership set for role " + roleName, e);
        }
        return null;
    }

    public boolean addMembership(String roleName, String userName) {
        try {
            api.path(routes.RolesResource().addMember(roleName, userName)).execute();
            return true;
        } catch (APIException | IOException e) {
            log.error("Unable to add role {} to user {}: {}", roleName, userName, e);
            return false;
        }
    }

    public boolean removeMembership(String roleName, String userName) {
        try {
            api.path(routes.RolesResource().removeMember(roleName, userName)).execute();
            return true;
        } catch (APIException | IOException e) {
            log.error("Unable to remove role {} from user {}: {}", roleName, userName, e);
            return false;
        }
    }

    public RoleResponse updateRole(RoleResponse role) {
        try {
            final RoleResponse response = api.path(routes.RolesResource().update(role.name()), RoleResponse.class).body(
                    role).execute();
            return response;
        } catch (APIException | IOException e) {
            log.error("Unable to update role " + role.name(), e);
        }
        return null;
    }

    public boolean deleteRole(String roleName) {
        try {
            api.path(routes.RolesResource().delete(roleName)).execute();
            return true;
        } catch (APIException | IOException e) {
            log.error("Unable to delete role " + roleName, e);
        }
        return false;
    }
}
