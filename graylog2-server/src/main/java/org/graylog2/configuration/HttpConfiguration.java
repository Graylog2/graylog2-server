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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

@DocumentationSection(heading = "HTTP settings", description = "")
public class HttpConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(HttpConfiguration.class);

    private static final int GRAYLOG_DEFAULT_PORT = 9000;

    public static final String OVERRIDE_HEADER = "X-Graylog-Server-URL";
    public static final String API_PREFIX = "api";
    public static final String PATH_API = API_PREFIX + "/";

    @Documentation("""
            ## HTTP bind address

            The network interface used by the Graylog HTTP interface.

            This network interface must be accessible by all Graylog nodes in the cluster and by all clients
            using the Graylog web interface.

            If the port is omitted, Graylog will use port 9000 by default.

            Default: 127.0.0.1:9000
            IPv6 example: http_bind_address = [2001:db8::1]:9000
            """)
    @Parameter(value = "http_bind_address", required = true)
    private HostAndPort httpBindAddress = HostAndPort.fromParts("127.0.0.1", GRAYLOG_DEFAULT_PORT);

    @Documentation("""
            ## HTTP publish URI

            The HTTP URI of this Graylog node which is used to communicate with the other Graylog nodes in the cluster and by all
            clients using the Graylog web interface.

            The URI will be published in the cluster discovery APIs, so that other Graylog nodes will be able to find and connect to this Graylog node.

            This configuration setting has to be used if this Graylog node is available on another network interface than $http_bind_address,
            for example if the machine has multiple network interfaces or is behind a NAT gateway.

            If $http_bind_address contains a wildcard IPv4 address (0.0.0.0), the first non-loopback IPv4 address of this machine will be used.
            This configuration setting *must not* contain a wildcard address!

            Default: http://$http_bind_address/
            """)
    @Parameter(value = "http_publish_uri", validator = URIAbsoluteValidator.class)
    private URI httpPublishUri;

    @Documentation("""
            ## Enable CORS headers for HTTP interface

            This allows browsers to make Cross-Origin requests from any origin.
            This is disabled for security reasons and typically only needed if running graylog
            with a separate server for frontend development.

            Default: false
            """)
    @Parameter(value = "http_enable_cors")
    private boolean httpEnableCors = false;

    @Documentation("""
            ## Enable GZIP support for HTTP interface

            This compresses API responses and therefore helps to reduce
            overall round trip times. This is enabled by default. Uncomment the next line to disable it.
            """)
    @Parameter(value = "http_enable_gzip")
    private boolean httpEnableGzip = true;

    @Documentation("The maximum size of the HTTP request headers in bytes.")
    @Parameter(value = "http_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpMaxHeaderSize = 8192;

    @Documentation("The size of the thread pool used exclusively for serving the HTTP interface.")
    @Parameter(value = "http_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpThreadPoolSize = 64;

    @Documentation("tbd")
    @Parameter(value = "http_selector_runners_count", required = true, validator = PositiveIntegerValidator.class)
    private int httpSelectorRunnersCount = 1;

    @Documentation("""
            ## Enable HTTPS support for the HTTP interface

            This secures the communication with the HTTP interface with TLS to prevent request forgery and eavesdropping.

            Default: false
            """)
    @Parameter(value = "http_enable_tls")
    private boolean httpEnableTls = false;

    @Documentation("The X.509 certificate chain file in PEM format to use for securing the HTTP interface.")
    @Parameter(value = "http_tls_cert_file")
    private Path httpTlsCertFile;

    @Documentation("The PKCS#8 private key file in PEM format to use for securing the HTTP interface.")
    @Parameter(value = "http_tls_key_file")
    private Path httpTlsKeyFile;

    @Documentation("The password to unlock the private key used for securing the HTTP interface.")
    @Parameter(value = "http_tls_key_password")
    private String httpTlsKeyPassword;

    @Documentation("""
            ## External Graylog URI

            The public URI of Graylog which will be used by the Graylog web interface to communicate with the Graylog REST API.

            The external Graylog URI usually has to be specified, if Graylog is running behind a reverse proxy or load-balancer
            and it will be used to generate URLs addressing entities in the Graylog REST API (see $http_bind_address).

            When using Graylog Collector, this URI will be used to receive heartbeat messages and must be accessible for all collectors.

            This setting can be overridden on a per-request basis with the "X-Graylog-Server-URL" HTTP request header.

            Default: $http_publish_uri
            """)
    @Parameter(value = "http_external_uri")
    private URI httpExternalUri;

    @Documentation("tbd")
    @Parameter(value = "http_allow_embedding")
    private boolean httpAllowEmbedding = false;

    @Documentation("tbd")
    @Parameter(value = "http_cookie_secure_override")
    private boolean httpCookieSecureOverride = false;

    @Documentation("tbd")
    @Parameter(value = "http_cookie_same_site_strict")
    private boolean httpCookieSameSiteStrict = true;

    public HostAndPort getHttpBindAddress() {
        return httpBindAddress
                .requireBracketsForIPv6()
                .withDefaultPort(GRAYLOG_DEFAULT_PORT);
    }

    public String getUriScheme() {
        return isHttpEnableTls() ? "https" : "http";
    }

    @Nullable
    private InetAddress toInetAddress(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            LOG.debug("Couldn't resolve \"{}\"", host, e);
            return null;
        }
    }

    public URI getHttpPublishUri() {
        if (httpPublishUri == null) {
            final URI defaultHttpUri = getDefaultHttpUri();
            LOG.debug("No \"http_publish_uri\" set. Using default <{}>.", defaultHttpUri);
            return defaultHttpUri;
        } else {
            final InetAddress inetAddress = toInetAddress(httpPublishUri.getHost());
            if (Tools.isWildcardInetAddress(inetAddress)) {
                final URI defaultHttpUri = getDefaultHttpUri(httpPublishUri.getPath());
                LOG.warn("\"{}\" is not a valid setting for \"http_publish_uri\". Using default <{}>.", httpPublishUri, defaultHttpUri);
                return defaultHttpUri;
            } else {
                return Tools.normalizeURI(httpPublishUri, httpPublishUri.getScheme(), GRAYLOG_DEFAULT_PORT, httpPublishUri.getPath());
            }
        }
    }

    @VisibleForTesting
    URI getDefaultHttpUri() {
        return getDefaultHttpUri("/");
    }

    private URI getDefaultHttpUri(String path) {
        final HostAndPort bindAddress = getHttpBindAddress();

        final URI publishUri;
        final InetAddress inetAddress = toInetAddress(bindAddress.getHost());
        if (inetAddress != null && Tools.isWildcardInetAddress(inetAddress)) {
            final InetAddress guessedAddress;
            try {
                guessedAddress = Tools.guessPrimaryNetworkAddress(inetAddress instanceof Inet4Address);

                if (guessedAddress.isLoopbackAddress()) {
                    LOG.debug("Using loopback address {}", guessedAddress);
                }
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for \"http_publish_uri\". Please configure it in your Graylog configuration.", e);
                throw new ParameterException("No http_publish_uri.", e);
            }

            try {
                publishUri = new URI(
                        getUriScheme(),
                        null,
                        guessedAddress.getHostAddress(),
                        bindAddress.getPort(),
                        path,
                        null,
                        null
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid http_publish_uri.", e);
            }
        } else {
            try {
                publishUri = new URI(
                        getUriScheme(),
                        null,
                        getHttpBindAddress().getHost(),
                        getHttpBindAddress().getPort(),
                        path,
                        null,
                        null
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid http_publish_uri.", e);
            }
        }

        return publishUri;
    }

    public boolean isHttpEnableCors() {
        return httpEnableCors;
    }

    public boolean isHttpEnableGzip() {
        return httpEnableGzip;
    }

    public int getHttpMaxHeaderSize() {
        return httpMaxHeaderSize;
    }

    public int getHttpThreadPoolSize() {
        return httpThreadPoolSize;
    }

    public int getHttpSelectorRunnersCount() {
        return httpSelectorRunnersCount;
    }

    public boolean isHttpEnableTls() {
        return httpEnableTls;
    }

    public Path getHttpTlsCertFile() {
        return httpTlsCertFile;
    }

    public Path getHttpTlsKeyFile() {
        return httpTlsKeyFile;
    }

    public String getHttpTlsKeyPassword() {
        return httpTlsKeyPassword;
    }

    public boolean getHttpCookieSameSiteStrict() {
        return httpCookieSameSiteStrict;
    }

    public boolean getHttpCookieSecureOverride() {
        return httpCookieSecureOverride;
    }

    public URI getHttpExternalUri() {
        return httpExternalUri == null ? getHttpPublishUri() : httpExternalUri;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateHttpBindAddress() throws ValidationException {
        try {
            final String host = getHttpBindAddress().getHost();
            if (!InetAddresses.isInetAddress(host)) {
                final InetAddress inetAddress = InetAddress.getByName(host);
            }
        } catch (IllegalArgumentException | UnknownHostException e) {
            throw new ValidationException(e);
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateHttpPublishUriPathEndsWithSlash() throws ValidationException {
        if (!getHttpPublishUri().getPath().endsWith("/")) {
            throw new ValidationException("\"http_publish_uri\" must end with a slash (\"/\")");
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateHttpExternalUriPathEndsWithSlash() throws ValidationException {
        if (!getHttpExternalUri().getPath().endsWith("/")) {
            throw new ValidationException("\"http_external_uri\" must end with a slash (\"/\")");
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateTlsConfig() throws ValidationException {
        if (isHttpEnableTls()) {
            if (!isRegularFileAndReadable(getHttpTlsKeyFile())) {
                throw new ValidationException("Unreadable or missing HTTP private key: " + getHttpTlsKeyFile());
            }

            if (!isRegularFileAndReadable(getHttpTlsCertFile())) {
                throw new ValidationException("Unreadable or missing HTTP X.509 certificate: " + getHttpTlsCertFile());
            }
        }
    }

    private boolean isRegularFileAndReadable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path);
    }
}
