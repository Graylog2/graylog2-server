package org.graylog2.radio.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.shared.security.ShiroSecurityContext;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public class RadioSecurityContextFactory implements SecurityContextFactory {
    public class RadioSecurityContext extends ShiroSecurityContext {
        public RadioSecurityContext(Subject subject, AuthenticationToken token) {
            super(subject, token, true, "radio");
        }
    }
    public class RadioPrinipal implements Principal {
        @Override
        public boolean equals(Object another) {
            if (another instanceof RadioPrinipal)
                return true;
            else
                return false;
        }

        @Override
        public String toString() {
            return this.getClass().getCanonicalName();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String getName() {
            return "radio";
        }
    }

    public class RadioSubject implements Subject {
        @Override
        public Object getPrincipal() {
            return new RadioPrinipal();
        }

        @Override
        public PrincipalCollection getPrincipals() {
            return null;
        }

        @Override
        public boolean isPermitted(String permission) {
            return true;
        }

        @Override
        public boolean isPermitted(Permission permission) {
            return true;
        }

        @Override
        public boolean[] isPermitted(String... permissions) {
            return new boolean[0];
        }

        @Override
        public boolean[] isPermitted(List<Permission> permissions) {
            return new boolean[0];
        }

        @Override
        public boolean isPermittedAll(String... permissions) {
            return true;
        }

        @Override
        public boolean isPermittedAll(Collection<Permission> permissions) {
            return true;
        }

        @Override
        public void checkPermission(String permission) throws AuthorizationException {

        }

        @Override
        public void checkPermission(Permission permission) throws AuthorizationException {

        }

        @Override
        public void checkPermissions(String... permissions) throws AuthorizationException {

        }

        @Override
        public void checkPermissions(Collection<Permission> permissions) throws AuthorizationException {

        }

        @Override
        public boolean hasRole(String roleIdentifier) {
            return true;
        }

        @Override
        public boolean[] hasRoles(List<String> roleIdentifiers) {
            return new boolean[0];
        }

        @Override
        public boolean hasAllRoles(Collection<String> roleIdentifiers) {
            return true;
        }

        @Override
        public void checkRole(String roleIdentifier) throws AuthorizationException {

        }

        @Override
        public void checkRoles(Collection<String> roleIdentifiers) throws AuthorizationException {

        }

        @Override
        public void checkRoles(String... roleIdentifiers) throws AuthorizationException {

        }

        @Override
        public void login(AuthenticationToken token) throws AuthenticationException {

        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public boolean isRemembered() {
            return true;
        }

        @Override
        public Session getSession() {
            return null;
        }

        @Override
        public Session getSession(boolean create) {
            return null;
        }

        @Override
        public void logout() {

        }

        @Override
        public <V> V execute(Callable<V> callable) throws ExecutionException {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public <V> Callable<V> associateWith(Callable<V> callable) {
            return null;
        }

        @Override
        public Runnable associateWith(Runnable runnable) {
            return null;
        }

        @Override
        public void runAs(PrincipalCollection principals) throws NullPointerException, IllegalStateException {

        }

        @Override
        public boolean isRunAs() {
            return false;
        }

        @Override
        public PrincipalCollection getPreviousPrincipals() {
            return null;
        }

        @Override
        public PrincipalCollection releaseRunAs() {
            return null;
        }
    }
    @Override
    public SecurityContext create(String userName, String credential, boolean isSecure, String authcScheme, String host) {
        final Subject subject = new RadioSubject();
        final AuthenticationToken authenticationToken = new AuthenticationToken() {
            @Override
            public Object getPrincipal() {
                return new RadioPrinipal();
            }

            @Override
            public Object getCredentials() {
                return null;
            }
        };
        return new RadioSecurityContext(subject, authenticationToken);
    }
}
