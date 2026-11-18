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
package org.graylog2.shared.rest;

import com.github.joschi.jadconfig.util.Duration;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.graylog2.configuration.HttpConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StrictTransportSecurityFilterTest {
    private static final String HEADER = StrictTransportSecurityFilter.STRICT_TRANSPORT_SECURITY;

    @Test
    void addsHeaderOnSecureRequestWhenEnabled() {
        final ContainerResponse response = filter(config(true, true, false), true);

        assertThat(response.getHeaderString(HEADER)).isEqualTo("max-age=31536000");
    }

    @Test
    void addsIncludeSubDomainsDirective() {
        final ContainerResponse response = filter(config(true, true, true), true);

        assertThat(response.getHeaderString(HEADER)).isEqualTo("max-age=31536000; includeSubDomains");
    }

    @Test
    void omitsHeaderWhenDisabled() {
        final ContainerResponse response = filter(config(false, true, false), true);

        assertThat(response.getHeaders()).doesNotContainKey(HEADER);
    }

    @Test
    void omitsHeaderWhenTlsIsDisabled() {
        final ContainerResponse response = filter(config(true, false, false), true);

        assertThat(response.getHeaders()).doesNotContainKey(HEADER);
    }

    @Test
    void omitsHeaderOnInsecureRequest() {
        final ContainerResponse response = filter(config(true, true, false), false);

        assertThat(response.getHeaders()).doesNotContainKey(HEADER);
    }

    private static HttpConfiguration config(boolean hstsEnabled, boolean tlsEnabled, boolean includeSubdomains) {
        final HttpConfiguration configuration = mock(HttpConfiguration.class);
        when(configuration.isHttpHstsEnabled()).thenReturn(hstsEnabled);
        when(configuration.isHttpEnableTls()).thenReturn(tlsEnabled);
        when(configuration.getHttpHstsMaxAge()).thenReturn(Duration.days(365));
        when(configuration.isHttpHstsIncludeSubdomains()).thenReturn(includeSubdomains);
        return configuration;
    }

    private static ContainerResponse filter(HttpConfiguration configuration, boolean secure) {
        final ContainerRequest request = new ContainerRequest(URI.create("https://localhost"), URI.create("/"), HttpMethod.GET, null, new MapPropertiesDelegate(), null);
        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.isSecure()).thenReturn(secure);
        request.setSecurityContext(securityContext);

        final ContainerResponse response = new ContainerResponse(request, Response.ok().build());
        new StrictTransportSecurityFilter(configuration).filter(request, response);
        return response;
    }
}
