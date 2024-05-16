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
package org.graylog2.rest.resources.system;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.models.system.sessions.responses.SessionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieFactoryTest {

    @Mock
    SessionResponse sessionResponse;

    @Mock
    ContainerRequest containerRequest;

    @BeforeEach
    void setUp() {
        when(sessionResponse.getAuthenticationToken()).thenReturn("secret-auth-value");
        when(sessionResponse.validUntil()).thenReturn(new Date());
        when(containerRequest.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    @Test
    void defaultPath() {
        final CookieFactory cookieFactory = new CookieFactory(new HttpConfiguration());
        final NewCookie cookie = cookieFactory.createAuthenticationCookie(sessionResponse, containerRequest);

        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void pathFromConfig() throws Exception {
        final HttpConfiguration httpConfiguration = new HttpConfiguration();
        new JadConfig(new InMemoryRepository(
                Map.of("http_external_uri", "http://graylog.local/path/from/config/")), httpConfiguration)
                .process();

        System.out.println(httpConfiguration.getHttpExternalUri());

        final CookieFactory cookieFactory = new CookieFactory(httpConfiguration);
        final NewCookie cookie = cookieFactory.createAuthenticationCookie(sessionResponse, containerRequest);

        assertThat(cookie.getPath()).isEqualTo("/path/from/config/");
    }

    @Test
    void pathFromRequest() {
        containerRequest.getHeaders().put(HttpConfiguration.OVERRIDE_HEADER,
                List.of("http://graylog.local/path/from/request/"));

        final CookieFactory cookieFactory = new CookieFactory(new HttpConfiguration());
        final NewCookie cookie = cookieFactory.createAuthenticationCookie(sessionResponse, containerRequest);

        assertThat(cookie.getPath()).isEqualTo("/path/from/request/");
    }

    @Test
    void safePath() {
        containerRequest.getHeaders().put(HttpConfiguration.OVERRIDE_HEADER,
                List.of("http://graylog.local/path/;authentication=overridden-auth-value;"));

        final CookieFactory cookieFactory = new CookieFactory(new HttpConfiguration());
        final NewCookie cookie = cookieFactory.createAuthenticationCookie(sessionResponse, containerRequest);


        final String cookieString =
                RuntimeDelegate.getInstance().createHeaderDelegate(NewCookie.class).toString(cookie);

        final Cookie parsedCookie =
                RuntimeDelegate.getInstance().createHeaderDelegate(Cookie.class).fromString(cookieString);

        assertThat(parsedCookie.getName()).isEqualTo("authentication");
        assertThat(parsedCookie.getValue()).isEqualTo("secret-auth-value");
        assertThat(cookie.getPath()).isEqualTo("/path/authentication=overridden-auth-value/");
    }
}
