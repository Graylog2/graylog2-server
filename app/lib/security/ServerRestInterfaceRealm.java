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
package lib.security;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lib.APIException;
import lib.ApiClient;
import models.User;
import models.UserService;
import models.api.responses.system.UserResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.PlayException;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

/**
 * Shiro Realm implementation that uses a Graylog2-server as the source of the subject's information.
 */
@Singleton
public class ServerRestInterfaceRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(ServerRestInterfaceRealm.class);
    private final ApiClient api;
    private final User.Factory userFactory;

    @Inject
    private ServerRestInterfaceRealm(ApiClient api, User.Factory userFactory) {
        this.api = api;
        this.userFactory = userFactory;
        setAuthenticationTokenClass(SessionIdAuthenticationToken.class);
        // when requesting the current user does not fail with the session id we have, then we are authenticated.
        setCredentialsMatcher(new AllowAllCredentialsMatcher());
        setCachingEnabled(false);
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // the current user has been previously loaded via doGetAuthenticationInfo before, use those permissions
        final User currentUser = UserService.current();
        if (!principals.getPrimaryPrincipal().equals(currentUser.getName())) {
            log.error("The requested principal is not the current user! TODO load the user");
            return null;
        }
        final List<String> permissions = currentUser.getPermissions();
        final SimpleAuthorizationInfo authzInfo = new SimpleAuthorizationInfo();
        authzInfo.setStringPermissions(Sets.newHashSet(permissions));
        return authzInfo;
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
        final UserResponse response;

        // we don't handle any other type, see constructor
        @SuppressWarnings("CastToConcreteClass")
        SessionIdAuthenticationToken token = (SessionIdAuthenticationToken) authToken;
        try {
            final String sessionId = token.getPrincipal().toString();
            response = api.get(UserResponse.class)
                    .path("/users/{0}", token.getUsername())
                    .session(sessionId)
                    .execute();
            final User user = userFactory.fromResponse(response, sessionId);

            UserService.setCurrent(user);
            user.setSubject(new Subject.Builder(SecurityUtils.getSecurityManager())
                    .principals(new SimplePrincipalCollection(user.getName(), "REST realm"))
                    .authenticated(true)
                    .buildSubject());
        } catch (IOException e) {
            throw new Graylog2ServerUnavailableException("Could not connect to Graylog2 Server.", e);
        } catch (APIException e) {
            if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                throw new Graylog2ServerUnavailableException("Could not connect to Graylog2 Server.", e);
            } else {
                throw new AuthenticationException("Unable to communicate with graylog2-server backend", e);
            }
        } catch (PlayException e) {
            log.error("Misconfigured play application. Please make sure your application.secret is longer than 16 characters!", e);
            throw new RuntimeException(e);
        }
        return new SimpleAuthenticationInfo(response.username, null, "rest-interface");
    }
}
