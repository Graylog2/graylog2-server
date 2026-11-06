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
package org.graylog.mcp.oauth;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MultivaluedMap;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.RestTools;

import java.net.URI;
import java.util.Locale;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Builds the externally visible URLs of the MCP OAuth endpoints.
 */
@Singleton
public class McpOAuthUrls {
    public static final String MCP_PATH = "api/mcp";
    public static final String CONSENT_PAGE_PATH = "oauth-consent";

    private final URI httpExternalUri;

    @Inject
    public McpOAuthUrls(HttpConfiguration httpConfiguration) {
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
    }

    public Urls forRequest(MultivaluedMap<String, String> headers) {
        final URI externalUri = RestTools.buildExternalUri(headers, httpExternalUri);
        return new Urls(externalUri.isAbsolute() ? externalUri : httpExternalUri);
    }

    /**
     * Checks whether a {@code resource} parameter (RFC 8707) refers to this server's MCP endpoint. Only the path is
     * compared, because clients may reach the server through different host names.
     */
    public static boolean isMcpResource(String resource) {
        try {
            final URI uri = URI.create(resource);
            if (!uri.isAbsolute() || uri.getRawFragment() != null || uri.getPath() == null) {
                return false;
            }
            final String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            return (scheme.equals("http") || scheme.equals("https")) && stripTrailingSlash(uri.getPath()).endsWith("/" + MCP_PATH);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record Urls(URI base) {
        public Urls {
            if (!base.toString().endsWith("/")) {
                base = URI.create(base + "/");
            }
        }

        public String issuer() {
            return stripTrailingSlash(base.toString());
        }

        public String resource() {
            return base.resolve(MCP_PATH).toString();
        }

        public String authorizationEndpoint() {
            return base.resolve("oauth/authorize").toString();
        }

        public String tokenEndpoint() {
            return base.resolve("oauth/token").toString();
        }

        public String registrationEndpoint() {
            return base.resolve("oauth/register").toString();
        }

        public String revocationEndpoint() {
            return base.resolve("oauth/revoke").toString();
        }

        public String consentPage() {
            return base.resolve(CONSENT_PAGE_PATH).toString();
        }

        /**
         * RFC 9728 section 3.1: the well-known segment goes between host and path of the resource.
         */
        public String protectedResourceMetadata() {
            return wellKnown("oauth-protected-resource", URI.create(resource()).getPath());
        }

        /**
         * RFC 8414 section 3.1: the well-known segment goes between host and path of the issuer.
         */
        public String authorizationServerMetadata() {
            return wellKnown("oauth-authorization-server", URI.create(issuer()).getPath());
        }

        private String wellKnown(String name, String path) {
            final String suffix = path == null ? "" : stripTrailingSlash(path);
            return f("%s://%s/.well-known/%s%s", base.getScheme(), base.getRawAuthority(), name, suffix);
        }
    }
}
