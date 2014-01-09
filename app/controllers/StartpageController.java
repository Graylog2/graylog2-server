/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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

import com.google.inject.Inject;
import models.Startpage;
import models.User;
import models.UserService;
import play.mvc.Call;
import play.mvc.Result;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StartpageController extends AuthenticatedController {

    @Inject
    private UserService userService;

    public Result redirect() {
        Startpage startpage = currentUser().getStartpage();

        Call call;
        if (startpage == null || startpage.getCall() == null) {
            call = routes.SystemController.index(0);
        } else {
            call = startpage.getCall();
        }

        return redirect(call);
    }

    public Result set(String pageType, String id) {
        Startpage.Type type = Startpage.Type.valueOf(pageType.toUpperCase());

        currentUser().setStartpage(new Startpage(type, id));

        Call redirectTarget;
        switch (type) {
            case STREAM:
                redirectTarget = routes.StreamsController.index();
                break;
            case DASHBOARD:
                redirectTarget = routes.DashboardsController.index();
                break;
            default:
                redirectTarget = routes.SystemController.index(0);
        }

        flash("success", "Configured new startpage for your user.");
        return redirect(redirectTarget);
    }

    public Result reset(String username) {
        User user = userService.load(username);
        user.setStartpage(null);

        flash("success", "Startpage of user was reset.");
        return redirect(routes.UsersController.editUserForm(user.getName()));
    }

}
