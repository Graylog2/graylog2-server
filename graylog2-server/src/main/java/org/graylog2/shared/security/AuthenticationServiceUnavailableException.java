package org.graylog2.shared.security;

import org.apache.shiro.authc.AuthenticationException;

/**
 * Thrown when authentication fails due to an external service being unavailable.
 */
public class AuthenticationServiceUnavailableException extends AuthenticationException {
    public AuthenticationServiceUnavailableException() {
        super();
    }

    public AuthenticationServiceUnavailableException(String message) {
        super(message);
    }

    public AuthenticationServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    public AuthenticationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
