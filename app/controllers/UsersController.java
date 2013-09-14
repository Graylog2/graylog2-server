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

import lib.BreadcrumbList;
import lib.Tools;
import models.CreateUserRequest;
import models.Permissions;
import models.User;
import models.Users;
import org.apache.shiro.crypto.hash.SimpleHash;
import play.data.Form;
import play.mvc.Result;

import java.util.List;

public class UsersController extends AuthenticatedController {

    private static final Form<CreateUserRequest> createUserForm = Form.form(CreateUserRequest.class);

    public static Result index() {
        final List<User> allUsers = Users.all();
        final List<String> permissions = Permissions.all();
        return ok(views.html.users.index.render(currentUser(), allUsers, permissions));
    }

    public static Result newUser() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Users", routes.UsersController.index());
        bc.addCrumb("Create new", routes.UsersController.newUser());

        final List<String> permissions = Permissions.all();
        return ok(views.html.users.new_user.render(currentUser(), permissions, bc));
    }

    public static Result create() {
        Form<CreateUserRequest> createUserRequestForm = Tools.bindMultiValueFormFromRequest(CreateUserRequest.class);
        final CreateUserRequest request = createUserRequestForm.get();

        if (createUserRequestForm.hasErrors()) {
            return newUser(); // TODO we really want to pass the filled-out form here. TehSuck
        }
        // hash it before sending it across
        request.password = new SimpleHash("SHA1", request.password).toString();
        Users.create(request);
        return redirect(routes.UsersController.index());
    }

    public static Result edit(String username) {
        User user = Users.loadUser(username);
        return ok(views.html.users.edit.render(currentUser(), user));
    }

    public static Result uniqueUsername(String username) {
        if (User.LocalAdminUser.getInstance().getName().equals(username)) {
            return noContent();
        }
        if (Users.loadUser(username) == null) {
            return notFound();
        } else {
            return noContent();
        }
    }
}
