package org.graylog2.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class ShiroSecurityContext implements SecurityContext {
    private static final Logger log = LoggerFactory.getLogger(ShiroSecurityContext.class);

    private final Subject subject;
    private final UsernamePasswordToken token;
    private final boolean secure;
    private final String authcScheme;

    public ShiroSecurityContext(Subject subject, UsernamePasswordToken token, boolean isSecure, String authcScheme) {
        this.subject = subject;
        this.token = token;
        secure = isSecure;
        this.authcScheme = authcScheme;
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return subject.getPrincipal().toString();
            }
        };
    }

    @Override
    public boolean isUserInRole(String role) {
        log.info("Checking role {} for user {}.", role, subject.getPrincipal());
        return subject.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return authcScheme;
    }

    public void loginSubject() throws AuthenticationException {
        subject.login(token);
    }
}
