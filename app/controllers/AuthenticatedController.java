/*
 * Copyright 2013 TORCH UG
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
 */
package controllers;

import lib.security.RedirectAuthenticator;
import org.graylog2.restclient.models.User;
import org.graylog2.restclient.models.UserService;
import play.mvc.Security.Authenticated;

import java.net.MalformedURLException;
import java.net.URL;

@Authenticated(RedirectAuthenticator.class)
public class AuthenticatedController extends BaseController {

	protected static User currentUser() {
        return UserService.current();
	}

    protected String getRefererPath() {
        try {
            URL parser = new URL(request().getHeader(REFERER));
            return parser.getPath();
        } catch (MalformedURLException e) {
            return "/";
        }
    }
}
