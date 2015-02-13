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
package lib.security;

import models.LocalAdminUser;
import org.graylog2.restclient.models.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.Subject;
import play.libs.Crypto;
import play.mvc.Http;

public class LocalAdminUserRealm extends SimpleAccountRealm {

    public LocalAdminUserRealm(String name) {
        super(name);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        final AuthenticationInfo authenticationInfo = super.doGetAuthenticationInfo(token);

        // if we could authenticate the user with this realm, it is the local admin user.
        // set the request's current user to be the shared instance, so we don't blow everything up trying to retrieve it from the server.

        if (authenticationInfo != null) {
            if (!(token instanceof UsernamePasswordToken)) {
                throw new IllegalStateException("Only supports UsernamePasswordToken");
            }
            UsernamePasswordToken userPass = (UsernamePasswordToken) token;

            UserService.setCurrent(LocalAdminUser.getInstance());
            final String sessionid = Crypto.encryptAES(userPass.getUsername() + "\t" + new String(userPass.getPassword()));
            Http.Context.current().session().put("sessionid", sessionid);
            new Subject.Builder(SecurityUtils.getSecurityManager())
                    .authenticated(true)
                    .buildSubject();
        }
        return authenticationInfo;
    }
}
