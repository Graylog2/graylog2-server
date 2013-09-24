/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
 *
 */
package controllers;

import com.google.common.collect.ImmutableSet;
import lib.BreadcrumbList;
import lib.Tools;
import models.Permissions;
import models.User;
import models.api.requests.ChangeUserRequest;
import models.api.requests.CreateUserRequest;
import org.apache.shiro.crypto.hash.SimpleHash;
import play.data.Form;
import play.mvc.Result;
import views.html.users.new_user;
import views.html.users.show;

import java.util.List;

public class UsersController extends AuthenticatedController {

    private static final Form<CreateUserRequest> createUserForm = Form.form(CreateUserRequest.class);
    private static final Form<ChangeUserRequest> changeUserForm = Form.form(ChangeUserRequest.class);

    public Result index() {
        final List<User> allUsers = User.all();
        final List<String> permissions = Permissions.all();
        return ok(views.html.users.index.render(currentUser(), allUsers, permissions));
    }

    public Result show(String username) {
        final User user = User.load(username);
        if (user == null) {
            return notFound();
        }

        BreadcrumbList bc = breadcrumbs();
        bc.addCrumb(user.getFullName(), routes.UsersController.show(username));

        return ok(show.render(user, currentUser(), bc));
    }

    public Result newUserForm() {
        BreadcrumbList bc = breadcrumbs();
        bc.addCrumb("Create new", routes.UsersController.newUserForm());

        final List<String> permissions = Permissions.all();
        return ok(new_user.render(createUserForm, currentUser(), permissions, ImmutableSet.<String>of(), bc));
    }

    public Result editUserForm(String username) {
        BreadcrumbList bc = breadcrumbs();
        bc.addCrumb("Edit " + username, routes.UsersController.editUserForm(username));

        User user = User.load(username);
        final Form<ChangeUserRequest> form = changeUserForm.fill(new ChangeUserRequest(user));
        return ok(views.html.users.edit.render(form, username, currentUser(), Permissions.all(), ImmutableSet.copyOf(user.getPermissions()), bc));
    }

    public Result create() {
        Form<CreateUserRequest> createUserRequestForm = Tools.bindMultiValueFormFromRequest(CreateUserRequest.class);
        final CreateUserRequest request = createUserRequestForm.get();

        if (createUserRequestForm.hasErrors()) {
            BreadcrumbList bc = breadcrumbs();
            bc.addCrumb("Create new", routes.UsersController.newUserForm());
            final List<String> permissions = Permissions.all();
            return badRequest(new_user.render(createUserRequestForm, currentUser(), permissions, ImmutableSet.copyOf(request.permissions), bc));
        }
        // hash it before sending it across
        request.password = new SimpleHash("SHA1", request.password).toString();
        User.create(request);
        return redirect(routes.UsersController.index());
    }

    public Result isUniqueUsername(String username) {
        if (User.LocalAdminUser.getInstance().getName().equals(username)) {
            return noContent();
        }
        if (User.load(username) == null) {
            return notFound();
        } else {
            return noContent();
        }
    }

    public Result saveChanges(String username) {
        final Form<ChangeUserRequest> requestForm = Tools.bindMultiValueFormFromRequest(ChangeUserRequest.class);
        if (requestForm.hasErrors()) {
            final BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Users", routes.UsersController.index());
            bc.addCrumb("Edit " + username, routes.UsersController.editUserForm(username));

            final List<String> all = Permissions.all();

            return badRequest(views.html.users.edit.render(requestForm, username, currentUser(), all, ImmutableSet.copyOf(requestForm.get().permissions), bc));
        }
        final User user = User.load(username);
        user.update(requestForm.get());

        return redirect(routes.UsersController.index());
    }

    private static BreadcrumbList breadcrumbs() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Users", routes.UsersController.index());
        return bc;
    }
}
