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

import lib.APIException;
import lib.ApiClient;
import models.User;
import models.api.responses.system.UserResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Crypto;
import play.mvc.Http;

import java.io.IOException;

/**
 * Shiro Realm implementation that uses a Graylog2-server as the source of the subject's information.
 */
public class ServerRestInterfaceRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(ServerRestInterfaceRealm.class);

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // TODO currently we don't have any authorization information yet :(
        return null;
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        final UserResponse response;
        if (!(authToken instanceof UsernamePasswordToken)) {
            throw new IllegalStateException("Expected UsernamePasswordToken");
        }
        UsernamePasswordToken token = (UsernamePasswordToken) authToken;
        try {
            final SimpleHash sha1 = new SimpleHash("SHA1", token.getPassword());
            final String passwordHash = sha1.toString();

            log.debug("Trying to log in {} via REST", token.getUsername());
            // TODO string concat in url sucks, use messageformat or something that actually encodes, too
            response = ApiClient.get(UserResponse.class)
                    .path("/users/{0}", token.getUsername())
                    .credentials(token.getUsername(), passwordHash)
                    .execute();
            final User user = new User(response, passwordHash);

            User.setCurrent(user);

            // well, "sessiondid"
            final String sessionid = Crypto.encryptAES(token.getUsername() + "\t" + passwordHash);
            Http.Context.current().session().put("sessionid", sessionid);
            new Subject.Builder(SecurityUtils.getSecurityManager())
                    .authenticated(true)
                    .buildSubject();
        } catch (IOException e) {
            throw new AuthenticationException("Unable to communicate with graylog2-server backend", e);
        } catch (APIException e) {
            throw new Graylog2ServerUnavailableException("Could not connect to Graylog2 Server.", e);
            // throw new AuthenticationException("Server responded with non-200 code", e);
        }
        return new SimpleAuthenticationInfo(response.username, authToken.getCredentials(), "rest-interface");
    }
}
