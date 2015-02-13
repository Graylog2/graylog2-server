/*
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
