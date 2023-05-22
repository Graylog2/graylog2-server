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
package org.graylog.datanode;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.IntegerConverter;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import org.graylog.datanode.configuration.BaseConfiguration;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Helper class to hold configuration of Graylog
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration extends BaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Parameter(value = "installation_source", validator = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Parameter(value = "skip_preflight_checks")
    private boolean skipPreflightChecks = false;

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Parameter(value = "is_leader")
    private boolean isLeader = true;

    @Parameter("disable_native_system_stats_collector")
    private boolean disableNativeSystemStatsCollector = false;

    @Parameter(value = "opensearch_location")
    private String opensearchDistributionRoot = "dist";

    @Parameter(value = "opensearch_data_location")
    private String opensearchDataLocation = "data";

    @Parameter(value = "opensearch_logs_location")
    private String opensearchLogsLocation = "logs";

    @Parameter(value = "opensearch_config_location")
    private String opensearchConfigLocation = "config";

    @Parameter(value = "config_location")
    private String configLocation;

    @Parameter(value = "process_logs_buffer_size")
    private Integer logs = 500;


    @Parameter(value = "node_name")
    private String datanodeNodeName = "node1";

    @Parameter(value = "opensearch_http_port", converter = IntegerConverter.class)
    private int opensearchHttpPort = 9200;


    @Parameter(value = "opensearch_transport_port", converter = IntegerConverter.class)
    private int opensearchTransportPort = 9300;

    @Parameter(value = "opensearch_discovery_seed_hosts", converter = StringListConverter.class)
    private List<String> opensearchDiscoverySeedHosts = Collections.emptyList();

    @Parameter(value = "opensearch_network_host")
    private String opensearchNetworkHostHost = null;

    @Parameter(value = "transport_certificate")
    private String datanodeTransportCertificate = "datanode-transport-certificates.p12";

    @Parameter(value = "transport_certificate_password")
    private String datanodeTransportCertificatePassword;

    @Parameter(value = "http_certificate")
    private String datanodeHttpCertificate = "datanode-http-certificates.p12";

    @Parameter(value = "http_certificate_password")
    private String datanodeHttpCertificatePassword;

    @Parameter(value = "stale_leader_timeout", validators = PositiveIntegerValidator.class)
    private Integer staleLeaderTimeout = 2000;

    @Parameter(value = "user_password_default_algorithm")
    private String userPasswordDefaultAlgorithm = "bcrypt";

    @Parameter(value = "user_password_bcrypt_salt_size", validators = PositiveIntegerValidator.class)
    private int userPasswordBCryptSaltSize = 10;

    public Integer getStaleLeaderTimeout() {
        return staleLeaderTimeout;
    }

    public String getInstallationSource() {
        return installationSource;
    }

    public boolean getSkipPreflightChecks() {
        return skipPreflightChecks;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public boolean isDisableNativeSystemStatsCollector() {
        return disableNativeSystemStatsCollector;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public String getOpensearchDistributionRoot() {
        return opensearchDistributionRoot;
    }

    public String getOpensearchConfigLocation() {
        return opensearchConfigLocation;
    }

    public String getConfigLocation() {
        return configLocation;
    }

    public String getOpensearchDataLocation() {
        return opensearchDataLocation;
    }

    public String getOpensearchLogsLocation() {
        return opensearchLogsLocation;
    }

    public Integer getProcessLogsBufferSize() {
        return logs;
    }

    @Parameter(value = "rest_api_username")
    private String restApiUsername;

    @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
    private String passwordSecret;

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validatePasswordSecret() throws ValidationException {
        if (passwordSecret == null || passwordSecret.length() < 16) {
            throw new ValidationException("The minimum length for \"password_secret\" is 16 characters.");
        }
    }

    @Parameter(value = "rest_api_password")
    private String restApiPassword;

    @Parameter(value = "node_id_file", validators = NodeIdFileValidator.class)
    private String nodeIdFile = "data/node-id";

    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    @Parameter(value = "root_timezone")
    private DateTimeZone rootTimeZone = DateTimeZone.UTC;

    @Parameter(value = "root_email")
    private String rootEmail = "";

    @Parameter(value = "single_node_only")
    private boolean singleNodeOnly = false;

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public DateTimeZone getRootTimeZone() {
        return rootTimeZone;
    }

    public String getRootEmail() {
        return rootEmail;
    }

    public String getDatanodeNodeName() {
        return datanodeNodeName;
    }

    public int getOpensearchHttpPort() {
        return opensearchHttpPort;
    }

    public int getOpensearchTransportPort() {
        return opensearchTransportPort;
    }

    public List<String> getOpensearchDiscoverySeedHosts() {
        return opensearchDiscoverySeedHosts;
    }

    public String getDatanodeTransportCertificate() {
        return datanodeTransportCertificate;
    }

    public String getDatanodeTransportCertificatePassword() {
        return datanodeTransportCertificatePassword;
    }

    public String getDatanodeHttpCertificate() {
        return datanodeHttpCertificate;
    }

    public String getDatanodeHttpCertificatePassword() {
        return datanodeHttpCertificatePassword;
    }


    public String getRestApiUsername() {
        return restApiUsername;
    }

    public String getRestApiPassword() {
        return restApiPassword;
    }


    public Optional<String> getOpensearchNetworkHostHost() {
        return Optional.ofNullable(opensearchNetworkHostHost);
    }

    public boolean isSingleNodeOnly() {
        return singleNodeOnly;
    }

    public static class NodeIdFileValidator implements Validator<String> {
        @Override
        public void validate(String name, String path) throws ValidationException {
            if (path == null) {
                return;
            }
            final File file = Paths.get(path).toFile();
            final StringBuilder b = new StringBuilder();

            if (!file.exists()) {
                final File parent = file.getParentFile();
                if (!parent.isDirectory()) {
                    throw new ValidationException("Parent path " + parent + " for Node ID file at " + path + " is not a directory");
                } else {
                    if (!parent.canRead()) {
                        throw new ValidationException("Parent directory " + parent + " for Node ID file at " + path + " is not readable");
                    }
                    if (!parent.canWrite()) {
                        throw new ValidationException("Parent directory " + parent + " for Node ID file at " + path + " is not writable");
                    }

                    // parent directory exists and is readable and writable
                    return;
                }
            }

            if (!file.isFile()) {
                b.append("a file");
            }
            final boolean readable = file.canRead();
            final boolean writable = file.canWrite();
            if (!readable) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("readable");
            }
            final boolean empty = file.length() == 0;
            if (!writable && readable && empty) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("writable, but it is empty");
            }
            if (b.length() == 0) {
                // all good
                return;
            }
            throw new ValidationException("Node ID file at path " + path + " isn't " + b + ". Please specify the correct path or change the permissions");
        }
    }

    private static final int GRAYLOG_DEFAULT_PORT = 8999;

    public static final String OVERRIDE_HEADER = "X-Graylog-Server-URL";
    public static final String PATH_WEB = "";
    public static final String PATH_API = "api/";

    @Parameter(value = "http_bind_address", required = true)
    private HostAndPort httpBindAddress = HostAndPort.fromParts("127.0.0.1", GRAYLOG_DEFAULT_PORT);

    @Parameter(value = "http_publish_uri", validator = URIAbsoluteValidator.class)
    private URI httpPublishUri;

    @Parameter(value = "http_enable_cors")
    private boolean httpEnableCors = false;

    @Parameter(value = "http_enable_gzip")
    private boolean httpEnableGzip = true;

    @Parameter(value = "http_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpMaxHeaderSize = 8192;

    @Parameter(value = "http_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpThreadPoolSize = 64;

    @Parameter(value = "http_selector_runners_count", required = true, validator = PositiveIntegerValidator.class)
    private int httpSelectorRunnersCount = 1;

    @Parameter(value = "http_enable_tls")
    private boolean httpEnableTls = false;

    @Parameter(value = "http_tls_cert_file")
    private Path httpTlsCertFile;

    @Parameter(value = "http_tls_key_file")
    private Path httpTlsKeyFile;

    @Parameter(value = "http_tls_key_password")
    private String httpTlsKeyPassword;

    @Parameter(value = "http_external_uri")
    private URI httpExternalUri;

    @Parameter(value = "http_allow_embedding")
    private boolean httpAllowEmbedding = false;

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
