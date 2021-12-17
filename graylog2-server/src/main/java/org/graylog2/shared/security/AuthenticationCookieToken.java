package org.graylog2.shared.security;

import org.apache.shiro.authc.HostAuthenticationToken;

public class AuthenticationCookieToken implements HostAuthenticationToken {
    private final String cookieValue;
    public static final String SESSION_COOKIE_NAME = "authentication";

    public AuthenticationCookieToken(String cookieValue) {
        this.cookieValue = cookieValue;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public String getCookieValue() {
        return cookieValue;
    }
}
