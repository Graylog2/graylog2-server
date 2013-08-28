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

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

public class PlayAuthenticationListener implements AuthenticationListener {
    private static final Logger log = LoggerFactory.getLogger(PlayAuthenticationListener.class);

    @Override
    public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
        final Http.Session session = Http.Context.current().session();
        log.warn("Authenticated user {}", info.getPrincipals().getPrimaryPrincipal());
    }

    @Override
    public void onFailure(AuthenticationToken token, AuthenticationException ae) {
        // TODO keep metric of failed logins. maybe log to some graylog instance
    }

    @Override
    public void onLogout(PrincipalCollection principals) {
        final Http.Session session = Http.Context.current().session();
        session.remove("sessionid");
    }
}
