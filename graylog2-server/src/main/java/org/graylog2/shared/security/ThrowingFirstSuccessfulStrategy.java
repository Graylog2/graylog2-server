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
package org.graylog2.shared.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.realm.Realm;

import java.util.Collection;

/**
 * An authentication strategy pretty much the same as the {@link FirstSuccessfulStrategy} with the difference that it
 * will memoize a {@link AuthenticationServiceUnavailableException} thrown by any attempt. It will rethrow this
 * exception on the final {@link #afterAllAttempts(AuthenticationToken, AuthenticationInfo)} call, if none of the
 * attempts were successful.
 * <p>
 * This way we can retain the fact that authentication probably failed due to a service being unavailable and not
 * because the user's credentials were wrong.
 */
public class ThrowingFirstSuccessfulStrategy extends FirstSuccessfulStrategy {

    private AuthenticationServiceUnavailableException unavailableException;

    /**
     * Clear a possible {@link AuthenticationServiceUnavailableException} because this strategy will be re-used for
     * multiple authentication processes.
     */
    @Override
    public AuthenticationInfo beforeAllAttempts(Collection<? extends Realm> realms,
            AuthenticationToken token) throws AuthenticationException {
        unavailableException = null;
        return super.beforeAllAttempts(realms, token);
    }

    /**
     * If the attempt failed due to an {@link AuthenticationServiceUnavailableException}, memoize that exception. Will
     * overwrite any previously memoized exception.
     */
    @Override
    public AuthenticationInfo afterAttempt(Realm realm, AuthenticationToken token, AuthenticationInfo singleRealmInfo,
            AuthenticationInfo aggregateInfo, Throwable t) throws AuthenticationException {
        if (t instanceof AuthenticationServiceUnavailableException) {
            unavailableException = (AuthenticationServiceUnavailableException) t;
        }
        return super.afterAttempt(realm, token, singleRealmInfo, aggregateInfo, t);
    }

    /**
     * If none of the attempts was successful and at least one of the attempts was throwing a
     * {@link AuthenticationServiceUnavailableException}, we'll re-throw this exception here.
     * @throws AuthenticationServiceUnavailableException if none of the attempts was successful and at least one of
     * them was throwing an exception of this type.
     */
    @Override
    public AuthenticationInfo afterAllAttempts(AuthenticationToken token,
            AuthenticationInfo aggregate) throws AuthenticationServiceUnavailableException {

        final AuthenticationInfo authenticationInfo = super.afterAllAttempts(token, aggregate);

        if (authenticationInfo == null && unavailableException != null) {
            throw unavailableException;
        }
        return authenticationInfo;
    }
}
