/**
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
 *
 */
package controllers;

import com.google.inject.Inject;
import org.graylog2.restclient.models.Startpage;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import play.mvc.Call;
import play.mvc.Result;
import views.helpers.StartpageRouteHelper;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StartpageController extends AuthenticatedController {

    @Inject
    private UserService userService;

    public Result redirect() {
        Startpage startpage = currentUser().getStartpage();

        Call call;
        if (startpage == null || StartpageRouteHelper.getCall(startpage) == null) {
            call = routes.SearchController.globalSearch();
        } else {
            call = StartpageRouteHelper.getCall(startpage);
        }

        return redirect(call);
    }

    public Result set(String pageType, String id) {
        Startpage.Type type = Startpage.Type.valueOf(pageType.toUpperCase());

        final boolean success = currentUser().setStartpage(new Startpage(type, id));
        if (success) {
            flash("success", "Configured new startpage for your user.");
        } else {
            flash("error", "Could not set new startpage for your user.");
        }

        Call redirectTarget;
        switch (type) {
            case STREAM:
                redirectTarget = routes.StreamsController.index();
                break;
            case DASHBOARD:
                redirectTarget = routes.DashboardsController.index();
                break;
            default:
                redirectTarget = routes.SearchController.globalSearch();
        }

        return redirect(redirectTarget);
    }

    public Result reset(String username) {
        User user = userService.load(username);
        if (user.setStartpage(null)) {
            flash("success", "Startpage of user was reset.");
        } else {
            flash("error", "Could not reset startpage.");
        }


        return redirect(routes.UsersController.editUserForm(user.getName()));
    }

}
