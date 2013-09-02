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
package lib.security;

import models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security.Authenticator;

public class RedirectAuthenticator extends Authenticator {
    private static final Logger log = LoggerFactory.getLogger(RedirectAuthenticator.class);

    @Override
    public String getUsername(Context ctx) {
        final User sessionUser = User.authenticateSessionUser();
        if (sessionUser == null) {
            return null;
        }
        return sessionUser.getName();
    }

    @Override
	public Result onUnauthorized(Context ctx) {
		return redirect(controllers.routes.SessionsController.index());
	}
	
}
