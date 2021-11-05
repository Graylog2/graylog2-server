package org.graylog.plugins.views.search.rest.contexts;

import org.apache.shiro.subject.Subject;
import org.glassfish.hk2.api.Factory;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.security.UserContext;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.ShiroPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class SearchUserFactory implements Factory<SearchUser> {
    private static final Logger LOG = LoggerFactory.getLogger(SearchUserFactory.class);

    @Context
    private UserContext userContext;

    @Context
    private SecurityContext securityContext;

    @Override
    public SearchUser provide() {
        return new SearchUser(userContext.getUser(), this::isPermitted, this::isPermitted);
    }

    protected Subject getSubject() {
        if (securityContext == null) {
            LOG.error("Cannot retrieve current subject, SecurityContext isn't set.");
            return null;
        }

        final Principal p = securityContext.getUserPrincipal();
        if (!(p instanceof ShiroPrincipal)) {
            final String msg = "Unknown SecurityContext class " + securityContext + ", cannot continue.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }

        final ShiroPrincipal principal = (ShiroPrincipal) p;
        return principal.getSubject();
    }

    protected boolean isPermitted(String permission, String instanceId) {
        return getSubject().isPermitted(permission + ":" + instanceId);
    }

    protected boolean isPermitted(String permission) {
        return getSubject().isPermitted(permission);
    }

    @Override
    public void dispose(SearchUser searchUser) {

    }
}
