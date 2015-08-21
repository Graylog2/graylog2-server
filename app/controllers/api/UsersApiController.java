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

package controllers.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import controllers.AuthenticatedController;
import lib.json.Json;
import lib.security.RestPermissions;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import org.graylog2.restclient.models.api.responses.system.UserResponse;
import play.mvc.BodyParser;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static lib.security.RestPermissions.USERS_LIST;
import static views.helpers.Permissions.isPermitted;

public class UsersApiController extends AuthenticatedController {
    private final UserService userService;

    @Inject
    public UsersApiController(UserService userService) {
        this.userService = userService;
    }

    public Result listUsers() {
        final List<User> allUsers = isPermitted(USERS_LIST) ? userService.all() : Lists.newArrayList(currentUser());
        final List<UserResponse> response = Lists.newArrayList();

        for (User user : allUsers) {
            final UserResponse userResponse = new UserResponse();
            userResponse.username = user.getName();
            userResponse.fullName = user.getFullName();
            userResponse.email = user.getEmail();
            userResponse.readonly = user.isReadonly();
            userResponse.external = user.isExternal();
            userResponse.permissions = user.getPermissions();
            userResponse.roles = user.getRoles();

            response.add(userResponse);
        }

        return ok(Json.toJsonString(response)).as(MediaType.JSON_UTF_8.toString());
    }

    public Result loadUser(String username) {
        if (!currentUser().getName().equals(username) && !Permissions.isPermitted(RestPermissions.USERS_LIST)) {
            return redirect(controllers.routes.StartpageController.redirect());
        }

        User user = userService.load(username);
        if (user != null) {
            return ok(Json.toJsonString(user)).as(MediaType.JSON_UTF_8.toString());
        } else {
            return notFound();
        }
    }

    public Result deleteUser(String username) {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return notFound();
        }

        userService.delete(username);
        return ok();
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result saveUserPreferences(String username) throws IOException {
        if (!Permissions.isPermitted(RestPermissions.USERS_EDIT, username)) {
            return redirect(controllers.routes.StartpageController.redirect());
        }

        Map<String, Object> preferences = Json.fromJson(request().body().asJson(), Map.class);
        Map<String, Object> normalizedPreferences = normalizePreferences(preferences);
        if (userService.savePreferences(username, normalizedPreferences)) {
            return ok();
        } else {
            // TODO: Really?
            return notFound();
        }
    }

    private Map<String, Object> normalizePreferences(Map<String, Object> preferences) {
        Map<String, Object> normalizedPreferences = Maps.newHashMap();
        // TODO: Move types into a static map once we have more preferences
        for (Map.Entry<String, Object> preference : preferences.entrySet()) {
            if (preference.getKey().equals("updateUnfocussed")) {
                normalizedPreferences.put(preference.getKey(), asBoolean(preference.getValue()));
            } else if (preference.getKey().equals("disableExpensiveUpdates")) {
                normalizedPreferences.put(preference.getKey(), asBoolean(preference.getValue()));
            } else if (preference.getKey().equals("enableSmartSearch")) {
                normalizedPreferences.put(preference.getKey(), asBoolean(preference.getValue()));
            }
        }
        return normalizedPreferences;
    }

    private static Boolean asBoolean(Object value) {
        final Boolean normalizedValue;
        if (value instanceof Boolean) {
            normalizedValue = (Boolean) value;
        } else {
            normalizedValue = Boolean.valueOf(value.toString());
        }
        return normalizedValue;
    }

    public Result isUniqueUsername(String username) {
        if (userService.load(username) == null) {
            return notFound();
        } else {
            return noContent();
        }
    }
}