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
package controllers;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lib.BreadcrumbList;
import lib.security.RestPermissions;
import org.apache.shiro.subject.Subject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.DateTools;
import org.graylog2.restclient.lib.Tools;
import org.graylog2.restclient.models.PermissionsService;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.requests.ChangePasswordRequest;
import org.graylog2.restclient.models.api.requests.ChangeUserRequest;
import org.graylog2.restclient.models.api.requests.ChangeUserRequestForm;
import org.graylog2.restclient.models.api.requests.CreateUserRequestForm;
import org.graylog2.restclient.models.dashboards.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Result;
import views.helpers.Permissions;
import views.html.system.users.edit;
import views.html.system.users.new_user;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static lib.security.RestPermissions.DASHBOARDS_EDIT;
import static lib.security.RestPermissions.DASHBOARDS_READ;
import static lib.security.RestPermissions.STREAMS_EDIT;
import static lib.security.RestPermissions.STREAMS_READ;
import static lib.security.RestPermissions.USERS_PERMISSIONSEDIT;

public class UsersController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(UsersController.class);

    private static final Form<CreateUserRequestForm> createUserForm = Form.form(CreateUserRequestForm.class);
    private static final Form<ChangeUserRequestForm> changeUserForm = Form.form(ChangeUserRequestForm.class);
    private static final Form<ChangePasswordRequest> changePasswordForm = Form.form(ChangePasswordRequest.class);

    private final UserService userService;
    private final PermissionsService permissionsService;
    private final StreamService streamService;
    private final DashboardService dashboardService;

    @Inject
    public UsersController(UserService userService, PermissionsService permissionsService, StreamService streamService, DashboardService dashboardService) {
        this.userService = userService;
        this.permissionsService = permissionsService;
        this.streamService = streamService;
        this.dashboardService = dashboardService;
    }

    public Result index() {
        final List<String> permissions = permissionsService.all();
        return ok(views.html.system.users.index.render(currentUser(), breadcrumbs(), permissions));
    }

    public Result newUserForm() {
        if (!Permissions.isPermitted(RestPermissions.USERS_CREATE)) {
            return redirect(routes.StartpageController.redirect());
        }

        BreadcrumbList bc = breadcrumbs();
        bc.addCrumb("New", routes.UsersController.newUserForm());

        final List<String> permissions = permissionsService.all();
        try {
            return ok(new_user.render(
                    createUserForm,
                    currentUser(),
                    permissions,
                    ImmutableSet.<String>of(),
                    DateTools.getGroupedTimezoneIds().asMap(),
                    DateTools.getApplicationTimeZone(),
                    streamService.all(),
                    bc));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch streams. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result editUserForm(String username) {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return redirect(routes.StartpageController.redirect());
        }

        BreadcrumbList bc = breadcrumbs();
        bc.addCrumb("Edit " + username, routes.UsersController.editUserForm(username));

        final User user = userService.load(username);
        if (user == null) {
            flash("error", "User '" + username + "' not found.");
            return redirect(routes.UsersController.index());
        }

        final Form<ChangeUserRequestForm> form = changeUserForm.fill(new ChangeUserRequestForm(user));
        boolean requiresOldPassword = checkRequireOldPassword(username);
        try {
            return ok(edit.render(
                            form,
                            username,
                            currentUser(),
                            user,
                            requiresOldPassword,
                            permissionsService.all(),
                            ImmutableSet.copyOf(user.getPermissions()),
                            DateTools.getGroupedTimezoneIds().asMap(),
                            streamService.all(),
                            dashboardService.getAll(),
                            bc)
            );
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch streams. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result create() {
        if (!Permissions.isPermitted(RestPermissions.USERS_CREATE)) {
            return redirect(routes.StartpageController.redirect());
        }

        Form<CreateUserRequestForm> createUserRequestForm = Tools.bindMultiValueFormFromRequest(CreateUserRequestForm.class);
        final CreateUserRequestForm request = createUserRequestForm.get();

        if (createUserRequestForm.hasErrors()) {
            BreadcrumbList bc = breadcrumbs();
            bc.addCrumb("Create new", routes.UsersController.newUserForm());
            final List<String> permissions = permissionsService.all();
            try {
                return badRequest(new_user.render(
                        createUserRequestForm,
                        currentUser(),
                        permissions,
                        ImmutableSet.copyOf(request.permissions),
                        DateTools.getGroupedTimezoneIds().asMap(),
                        DateTools.getApplicationTimeZone(),
                        streamService.all(),
                        bc));
            } catch (IOException e) {
                return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
            } catch (APIException e) {
                String message = "Could not fetch streams. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
                return status(504, views.html.errors.error.render(message, e, request()));
            }
        }
        if (request.admin) {
            request.permissions = Lists.newArrayList("*");
        } else {
            request.permissions = permissionsService.readerPermissions(request.username);
        }

        if (!userService.create(request.toApiRequest())) {
            flash("error", "Could not create user due to an internal error.");
        }
        return redirect(routes.UsersController.index());
    }

    public Result delete(String username) {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return redirect(routes.StartpageController.redirect());
        }

        userService.delete(username);
        return redirect(routes.UsersController.index());
    }

    public Result saveChanges(String username) {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Form<ChangeUserRequestForm> requestForm = Form.form(ChangeUserRequestForm.class).bindFromRequest();

        final User user = userService.load(username);
        if (user == null) {
            flash("error", "User '" + username + "' not found.");
            return redirect(routes.UsersController.index());
        }

        if (requestForm.hasErrors()) {
            final BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Users", routes.UsersController.index());
            bc.addCrumb("Edit " + username, routes.UsersController.editUserForm(username));

            final List<String> all = permissionsService.all();
            boolean requiresOldPassword = checkRequireOldPassword(username);

            try {
                return badRequest(edit.render(
                        requestForm,
                        username,
                        currentUser(),
                        user,
                        requiresOldPassword,
                        all,
                        ImmutableSet.copyOf(requestForm.get().permissions),
                        DateTools.getGroupedTimezoneIds().asMap(),
                        streamService.all(),
                        dashboardService.getAll(),
                        bc));
            } catch (IOException e) {
                return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
            } catch (APIException e) {
                String message = "Could not fetch streams. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
                return status(504, views.html.errors.error.render(message, e, request()));
            }
        }

        final ChangeUserRequestForm formData = requestForm.get();
        // translate session timeout value from form fields to millis
        if (!formData.session_timeout_never) {
            TimeUnit timeoutUnit;
            if (formData.timeout_unit != null) {
                try {
                    timeoutUnit = TimeUnit.valueOf(formData.timeout_unit.toUpperCase());
                    formData.sessionTimeoutMs = timeoutUnit.toMillis(formData.timeout);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown value for session timeout unit. Cannot set session timeout value.", e);
                }
            }
        } else {
            formData.sessionTimeoutMs = -1; // which translates to "never".
        }
        Set<String> permissions = Sets.newHashSet(user.getPermissions());
        // TODO this does not handle combined permissions like streams:edit,read:1,2 !
        // remove all streams:edit, streams:read permissions and add the ones from the form back.

        permissions = Sets.newHashSet(Sets.filter(permissions, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String input) {
                return (input != null) &&
                        !(input.startsWith(STREAMS_READ.getPermission()) || input.startsWith(STREAMS_EDIT.getPermission()) ||
                                input.startsWith(DASHBOARDS_READ.getPermission()) || input.startsWith(DASHBOARDS_EDIT.getPermission()));
            }
        }));
        for (String streampermission : formData.streampermissions) {
            permissions.add(RestPermissions.STREAMS_READ.getPermission() + ":" + streampermission);
        }
        for (String streameditpermission : formData.streameditpermissions) {
            permissions.add(RestPermissions.STREAMS_EDIT.getPermission() + ":" + streameditpermission);
        }
        for (String dashboardpermission : formData.dashboardpermissions) {
            permissions.add(RestPermissions.DASHBOARDS_READ.getPermission() + ":" + dashboardpermission);
        }
        for (String dashboardeditpermissions : formData.dashboardeditpermissions) {
            permissions.add(RestPermissions.DASHBOARDS_EDIT.getPermission() + ":" + dashboardeditpermissions);
        }
        final ChangeUserRequest changeRequest = formData.toApiRequest();
        changeRequest.permissions = Lists.newArrayList(permissions);
        user.update(changeRequest);

        flash("success", "User '" + user.getFullName() + "' was updated successfully");
        return redirect(routes.UsersController.index());
    }

    private boolean checkRequireOldPassword(String username) {
        boolean requiresOldPassword = true;
        final User currentUser = currentUser();
        final Subject subject = currentUser.getSubject();
        final String currentUserName = currentUser.getName();
        if (subject.isPermitted("users:passwordchange:*")) {
            // if own account, require old password, otherwise don't require it
            requiresOldPassword = currentUserName.equals(username);
        }
        return requiresOldPassword;
    }

    public Result changePassword(String username) {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Form<ChangePasswordRequest> requestForm = changePasswordForm.bindFromRequest("old_password", "password");

        final ChangePasswordRequest request = requestForm.get();
        final User user = userService.load(username);
        if (user == null) {
            flash("error", "User '" + username + "' not found.");
            return redirect(routes.UsersController.index());
        }


        if (checkRequireOldPassword(username) && request.old_password == null) {
            requestForm.reject("Old password is required.");
        }
        if (requestForm.hasErrors() || !user.updatePassword(request)) {
            flash("error", "Could not update the password.");
            return redirect(routes.UsersController.editUserForm(username));
        }

        flash("success", "Successfully changed the password for user '" + user.getFullName() + "'");
        return redirect(routes.UsersController.editUserForm(username));
    }

    public Result resetPermissions(String username) {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return redirect(routes.StartpageController.redirect());
        }

        final DynamicForm requestForm = Form.form().bindFromRequest();

        boolean isAdmin = false;
        final String field = requestForm.get("permissiontype");
        if (field != null && field.equalsIgnoreCase("admin")) {
            isAdmin = true;
        }

        final User user = userService.load(username);
        if (user == null) {
            flash("error", "User '" + username + "' not found.");
            return redirect(routes.UsersController.index());
        }

        if (!Permissions.isPermitted(USERS_PERMISSIONSEDIT) || user.isReadonly()) {
            flash("error", "Unable to change user role");
            return redirect(routes.UsersController.editUserForm(username));
        }

        final ChangeUserRequest changeRequest = new ChangeUserRequest(user);
        if (isAdmin) {
            changeRequest.permissions = Lists.newArrayList("*");
        } else {
            changeRequest.permissions = permissionsService.readerPermissions(username);
        }
        final boolean success = user.update(changeRequest);
        if (success) {
            flash("success", "Successfully changed user role for " + user.getFullName() + " to " + (isAdmin ? "administrator" : "reader") + " permissions.");
        } else {
            flash("error", "Unable to change user role for " + user.getFullName());
        }
        return redirect(routes.UsersController.editUserForm(username));
    }

    private static BreadcrumbList breadcrumbs() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Users", routes.UsersController.index());
        return bc;
    }
}
