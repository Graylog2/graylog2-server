package org.graylog2.shared.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.realm.Realm;

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
