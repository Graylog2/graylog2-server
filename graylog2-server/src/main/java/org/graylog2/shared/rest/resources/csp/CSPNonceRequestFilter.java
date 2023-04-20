package org.graylog2.shared.rest.resources.csp;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.UUID;

import static org.graylog2.shared.rest.resources.csp.CSPDynamicFeature.CSP_NONCE_PROPERTY;

public class CSPNonceRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final var cspNonce = generateNonce();
        requestContext.setProperty(CSP_NONCE_PROPERTY, cspNonce);
    }

    private String generateNonce() {
        return UUID.randomUUID().toString();
    }
}
