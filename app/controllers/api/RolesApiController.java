package controllers.api;

import com.google.common.net.MediaType;
import controllers.AuthenticatedController;
import lib.json.Json;
import lib.security.RestPermissions;
import org.graylog2.rest.models.roles.responses.RoleResponse;
import org.graylog2.restclient.models.RolesService;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static views.helpers.Permissions.isPermitted;

public class RolesApiController extends AuthenticatedController {

    private final RolesService rolesService;

    @Inject
    public RolesApiController(RolesService rolesService) {
        this.rolesService = rolesService;
    }

    public Result listRoles() {
        if (!isPermitted(RestPermissions.ROLES_READ)) {
            return forbidden();
        }

        final Set<RoleResponse> roleResponses = rolesService.loadAll();

        return jsonOk(roleResponses);
    }

    public Result createRole() {
        if (!isPermitted(RestPermissions.ROLES_CREATE)) {
            return forbidden();
        }

        RoleResponse newRole = Json.fromJson(request().body().asJson(), RoleResponse.class);
        if (isNullOrEmpty(newRole.name()) || newRole.permissions() == null || newRole.permissions().isEmpty()) {
            return badRequest("Missing fields");
        }
        final RoleResponse role = rolesService.create(newRole);
        if (role == null) {
            return internalServerError();
        }
        return jsonOk(role);
    }

    public Result loadRole(String rolename) {
        if (!isPermitted(RestPermissions.ROLES_READ, rolename)) {
            return forbidden();
        }
        final RoleResponse role = rolesService.load(rolename);
        if (role == null) {
            return internalServerError();
        }
        return jsonOk(role);
    }

    public Result deleteRole(String rolename) {
        if (!isPermitted(RestPermissions.ROLES_DELETE, rolename)) {
            return forbidden();
        }

        if (!rolesService.deleteRole(rolename)) {
            return internalServerError();
        }
        return noContent();
    }

    public Result addMember(String rolename, String username) {
        if (!isPermitted(RestPermissions.USERS_ROLESEDIT, username)) {
            return forbidden();
        }

        if (!rolesService.addMembership(rolename, username)) {
            return internalServerError();
        }
        return noContent();
    }

    public Result deleteMember(String rolename, String username) {
        if (!isPermitted(RestPermissions.USERS_ROLESEDIT, username)) {
            return forbidden();
        }

        if (!rolesService.removeMembership(rolename, username)) {
            return internalServerError();
        }
        return noContent();
    }

    private static Status jsonOk(Object returnValue) {
        return ok(Json.toJsonString(returnValue)).as(MediaType.JSON_UTF_8.toString());
    }
}
