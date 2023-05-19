/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.rest.resources.csp;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import java.lang.reflect.Method;

public class CSPDynamicFeature implements DynamicFeature {
    private static final Logger LOG = LoggerFactory.getLogger(CSPDynamicFeature.class);
    public static final String CSP_NONCE_PROPERTY = "cspNonce";
    private final CSPService cspService;

    @Inject
    public CSPDynamicFeature(@Context CSPService cspService) {
        this.cspService = cspService;
    }

    public String dynamicCspString() {
        return dynamicCspString(CSP.CSP_DEFAULT);
    }

    public String dynamicCspString(String staticCspString) {
        if (!staticCspString.contains("connect-src")) {
            return staticCspString + "connect-src " + cspService.connectSrcValue();
        }
        return staticCspString;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Method resourceMethod = resourceInfo.getResourceMethod();
        final Class<?> resourceClass = resourceInfo.getResourceClass();
        String cspValue = null;
        if (resourceClass != null && resourceClass.isAnnotationPresent(CSP.class)) {
            cspValue = dynamicCspString(resourceClass.getAnnotation(CSP.class).value());
            LOG.debug("CSP class annotation for {}: {}", resourceClass.getSimpleName(), cspValue);
        } else if (resourceMethod != null && resourceMethod.isAnnotationPresent(CSP.class)) {
            cspValue = dynamicCspString(resourceMethod.getAnnotation(CSP.class).value());
            LOG.debug("CSP method annotation for {}: {}", resourceMethod.getName(), cspValue);
        }
        if (cspValue != null) {
            context.register(new CSPNonceRequestFilter());
            context.register(new CSPResponseFilter(cspValue));
        }
    }

}
