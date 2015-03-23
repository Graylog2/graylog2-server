package org.graylog2.shared.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

@Priority(Priorities.AUTHORIZATION)
public class RestrictToMasterFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RestrictToMasterFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.warn("Rejected request that is only allowed against master nodes. Returning HTTP 403.");
        throw new ForbiddenException("Request is only allowed against master nodes.");
    }
}
