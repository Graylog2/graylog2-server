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
import com.github.joschi.jadconfig.converters.StringSetConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog2.Configuration.SafeClassesValidator;
import org.graylog2.configuration.Documentation;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to hold configuration of DataNode
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    public static final String TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY = "transport_certificate_password";
    public static final String HTTP_CERTIFICATE_PASSWORD_PROPERTY = "http_certificate_password";

    public static final int DATANODE_DEFAULT_PORT = 8999;
    public static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";

    @Documentation(visible = false)
    @Parameter(value = "installation_source", validators = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Deprecated
    @Documentation(visible = false)
    @Parameter(value = "insecure_startup")
    private boolean insecureStartup = false;

    @Documentation("Do not perform any preflight checks when starting Datanode.")
    @Parameter(value = "skip_preflight_checks")
    private boolean skipPreflightChecks = false;

    @Documentation("How many milliseconds should datanode wait for termination of all tasks during the shutdown.")
    @Parameter(value = "shutdown_timeout", validators = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Documentation("Directory where Datanode will search for an opensearch distribution.")
    @Parameter(value = "opensearch_location")
    private String opensearchDistributionRoot = "dist";

    @Documentation("Data directory of the embedded opensearch. Contains indices of the opensearch. May be pointed to an existing" +
            "opensearch directory during in-place migration to Datanode")
    @Parameter(value = "opensearch_data_location", required = true)
    private Path opensearchDataLocation = Path.of("datanode/data");

    @Documentation("Logs directory of the embedded opensearch")
    @Parameter(value = "opensearch_logs_location", required = true, validators = DirectoryWritableValidator.class)
    private Path opensearchLogsLocation = Path.of("datanode/logs");

    @Documentation("Configuration directory of the embedded opensearch. This is the directory where the opensearch" +
            "process will store its configuration files. Caution, each start of the Datanode will regenerate the complete content of the directory!")
    @Parameter(value = "opensearch_config_location", required = true, validators = DirectoryWritableValidator.class)
    private Path opensearchConfigLocation = Path.of("datanode/config");

    @Documentation("Source directory of the additional configuration files for the Datanode. Additional certificates can be provided here.")
    @Parameter(value = "config_location", validators = DirectoryReadableValidator.class)
    private Path configLocation = null;

    @Documentation(visible = false)
    @Parameter(value = "native_lib_dir", required = true)
    private Path nativeLibDir = Path.of("native_libs");

    @Documentation("How many log entries of the opensearch process should Datanode hold in memory and make accessible via API calls.")
    @Parameter(value = "process_logs_buffer_size")
    private Integer opensearchProcessLogsBufferSize = 500;


    @Documentation("Unique name of this Datanode instance. use this, if your node name should be different from the hostname that's found by programmatically looking it up")
    @Parameter(value = "node_name")
    private String datanodeNodeName;


    @Documentation("Comma separated list of opensearch nodes that are eligible as manager nodes.")
    @Parameter(value = "initial_cluster_manager_nodes")
    private String initialClusterManagerNodes;

    @Documentation("Opensearch heap memory. Initial and maxmium heap must be identical for OpenSearch, otherwise the boot fails. So it's only one config option")
    @Parameter(value = "opensearch_heap")
    private String opensearchHeap = "1g";

    @Documentation("HTTP port on which the embedded opensearch listens")
    @Parameter(value = "opensearch_http_port", converter = IntegerConverter.class)
    private int opensearchHttpPort = 9200;

    @Documentation("Transport port on which the embedded opensearch listens")
    @Parameter(value = "opensearch_transport_port", converter = IntegerConverter.class)
    private int opensearchTransportPort = 9300;

    @Documentation("Provides a list of the addresses of the master-eligible nodes in the cluster.")
    @Parameter(value = "opensearch_discovery_seed_hosts", converter = StringListConverter.class)
    private List<String> opensearchDiscoverySeedHosts = Collections.emptyList();

    @Documentation("Binds an OpenSearch node to an address. Use 0.0.0.0 to include all available network interfaces, or specify an IP address assigned to a specific interface. ")
    @Parameter(value = "opensearch_network_host")
    private String opensearchNetworkHost = null;

    @Documentation("Relative path (to config_location) to a keystore used for opensearch transport layer TLS")
    @Parameter(value = "transport_certificate")
    private String datanodeTransportCertificate = null;

    @Documentation("Password for a keystore defined in transport_certificate")
    @Parameter(value = TRANSPORT_CERTIFICATE_PASSWORD_PROPERTY)
    private String datanodeTransportCertificatePassword;

    @Documentation("Relative path (to config_location) to a keystore used for opensearch REST layer TLS")
    @Parameter(value = "http_certificate")
    private String datanodeHttpCertificate = null;

    @Documentation("Password for a keystore defined in http_certificate")
    @Parameter(value = HTTP_CERTIFICATE_PASSWORD_PROPERTY)
    private String datanodeHttpCertificatePassword;

    @Documentation("You MUST set a secret to secure/pepper the stored user passwords here. Use at least 64 characters." +
            "Generate one by using for example: pwgen -N 1 -s 96 \n" +
            "ATTENTION: This value must be the same on all Graylog and Datanode nodes in the cluster. " +
            "Changing this value after installation will render all user sessions and encrypted values in the database invalid. (e.g. encrypted access tokens)")
    @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
    private String passwordSecret;

    @Documentation("communication between Graylog and OpenSearch is secured by JWT. This configuration defines interval between token regenerations.")
    @Parameter(value = "indexer_jwt_auth_token_caching_duration")
    Duration indexerJwtAuthTokenCachingDuration = Duration.seconds(60);

    @Documentation("communication between Graylog and OpenSearch is secured by JWT. This configuration defines validity interval of JWT tokens.")
    @Parameter(value = "indexer_jwt_auth_token_expiration_duration")
    Duration indexerJwtAuthTokenExpirationDuration = Duration.seconds(180);

    @Documentation("The auto-generated node ID will be stored in this file and read after restarts. It is a good idea " +
            "to use an absolute file path here if you are starting Graylog DataNode from init scripts or similar.")
    @Parameter(value = "node_id_file", validators = NodeIdFileValidator.class)
    private String nodeIdFile = "data/node-id";

    @Documentation("HTTP bind address. The network interface used by the Graylog DataNode to bind all services.")
    @Parameter(value = "bind_address", required = true)
    private String bindAddress = DEFAULT_BIND_ADDRESS;


    @Documentation("HTTP port. The port where the DataNode REST api is listening")
    @Parameter(value = "datanode_http_port", required = true)
    private int datanodeHttpPort = DATANODE_DEFAULT_PORT;

    @Documentation(visible = false)
    @Parameter(value = "hostname")
    private String hostname = null;

    @Documentation("Name of the cluster that the embedded opensearch will form. Should be the same for all Datanodes in one cluster.")
    @Parameter(value = "clustername")
    private String clustername = "datanode-cluster";

    @Documentation("This configuration should be used if you want to connect to this Graylog DataNode's REST API and it is available on " +
            "another network interface than $http_bind_address, " +
            "for example if the machine has multiple network interfaces or is behind a NAT gateway.")
    @Parameter(value = "http_publish_uri", validators  = URIAbsoluteValidator.class)
    private URI httpPublishUri;


    @Documentation("Enable GZIP support for HTTP interface. This compresses API responses and therefore helps to reduce " +
            " overall round trip times.")
    @Parameter(value = "http_enable_gzip")
    private boolean httpEnableGzip = true;

    @Documentation("The maximum size of the HTTP request headers in bytes")
    @Parameter(value = "http_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpMaxHeaderSize = 8192;

    @Documentation("The size of the thread pool used exclusively for serving the HTTP interface.")
    @Parameter(value = "http_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int httpThreadPoolSize = 64;

    @Documentation(visible = false, value = "The Grizzly default value is equal to `Runtime.getRuntime().availableProcessors()` which doesn't make " +
            "sense for Graylog because we are not mainly a web server. " +
            "See \"Selector runners count\" at https://grizzly.java.net/bestpractices.html for details.")
    @Parameter(value = "http_selector_runners_count", required = true, validator = PositiveIntegerValidator.class)
    private int httpSelectorRunnersCount = 1;

    @Documentation(visible = false, value = "TODO: do we need this configuration? We control the decision based on preflight and CA configurations")
    @Parameter(value = "http_enable_tls")
    private boolean httpEnableTls = false;


    @Documentation(visible = false, value = "Classes considered safe to load by name. A set of prefixes matched against the fully qualified class name.")
    @Parameter(value = org.graylog2.Configuration.SAFE_CLASSES, converter = StringSetConverter.class, validators = SafeClassesValidator.class)
    private Set<String> safeClasses = Set.of("org.graylog.", "org.graylog2.");

    @Documentation(visible = false)
    @Parameter(value = "metrics_timestamp")
    private String metricsTimestamp = "timestamp";

    @Documentation(visible = false)
    @Parameter(value = "metrics_stream")
    private String metricsStream = "gl-datanode-metrics";

    @Documentation(visible = false)
    @Parameter(value = "metrics_retention", validators = PositiveDurationValidator.class)
    private Duration metricsRetention = Duration.days(14);

    @Documentation(visible = false)
    @Parameter(value = "metrics_daily_retention", validators = PositiveDurationValidator.class)
    private Duration metricsDailyRetention = Duration.days(365);

    @Documentation(visible = false)
    @Parameter(value = "metrics_daily_index")
    private String metricsDailyIndex = "gl-datanode-metrics-daily";

    @Documentation(visible = false)
    @Parameter(value = "metrics_policy")
    private String metricsPolicy = "gl-datanode-metrics-ism";

    @Documentation(value = "Cache size for searchable snaphots")
    @Parameter(value = "node_search_cache_size")
    private String searchCacheSize = "10gb";

    /**
     * <a href="https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system">See snapshot documentation</a>
     */
    @Documentation("Filesystem path where searchable snapshots should be stored")
    @Parameter(value = "path_repo", converter = StringListConverter.class)
    private List<String> pathRepo;

    @Documentation("This setting limits the number of clauses a Lucene BooleanQuery can have.")
    @Parameter(value = "opensearch_indices_query_bool_max_clause_count")
    private Integer indicesQueryBoolMaxClauseCount = 32768;

    @Documentation("The list of the opensearch node’s roles.")
    @Parameter(value = "node_roles", converter = StringListConverter.class)
    private List<String> nodeRoles = List.of("cluster_manager", "data", "ingest", "remote_cluster_client", "search");

    @Documentation(visible = false)
    @Parameter(value = "async_eventbus_processors")
    private int asyncEventbusProcessors = 2;

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }


    public Integer getIndicesQueryBoolMaxClauseCount() {
        return indicesQueryBoolMaxClauseCount;
    }

    @Documentation("Configures verbosity of embedded opensearch logs. Possible values OFF, FATAL, ERROR, WARN, INFO, DEBUG, and TRACE, default is INFO")
    @Parameter(value = "opensearch_logger_org_opensearch")
    private String opensearchDebug;

    public String getOpensearchDebug() {
        return opensearchDebug;
    }

    @Documentation("Configures opensearch audit log storage type. See https://opensearch.org/docs/2.13/security/audit-logs/storage-types/")
    @Parameter(value = "opensearch_plugins_security_audit_type")
    private String opensearchAuditLog;

    public String getOpensearchAuditLog() {
        return opensearchAuditLog;
    }

     /**
     * The insecure flag causes problems on many places. We should replace it with autosecurity option, that would
     * configure all the CA and certs automatically.
     */
    @Deprecated
    public boolean isInsecureStartup() {
        return insecureStartup;
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

    public String getOpensearchDistributionRoot() {
        return opensearchDistributionRoot;
    }

    /**
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    public Path getOpensearchConfigLocation() {
        return opensearchConfigLocation;
    }


    /**
     * This is a pointer to a directory holding configuration files (and certificates) for the datanode itself.
     * We treat it as read only for the datanode and should never persist anything in it.
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    @Nullable
    public Path getDatanodeConfigurationLocation() {
        return configLocation;
    }

    /**
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    public Path getOpensearchDataLocation() {
        return opensearchDataLocation;
    }

    /**
     * Use {@link DatanodeDirectories} to obtain a reference to this directory.
     */
    public Path getOpensearchLogsLocation() {
        return opensearchLogsLocation;
    }

    public Integer getProcessLogsBufferSize() {
        return opensearchProcessLogsBufferSize;
    }

    public String getPasswordSecret() {
        return passwordSecret;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validatePasswordSecret() throws ValidationException {
        if (passwordSecret == null || passwordSecret.length() < 64) {
            throw new ValidationException("The minimum length for \"password_secret\" is 64 characters.");
        }
    }

    public String getDatanodeNodeName() {
        return datanodeNodeName != null && !datanodeNodeName.isBlank() ? datanodeNodeName : getHostname();
    }

    public String getInitialClusterManagerNodes() {
        return initialClusterManagerNodes;
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

    public Optional<String> getOpensearchNetworkHost() {
        return Optional.ofNullable(opensearchNetworkHost);
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public int getDatanodeHttpPort() {
        return datanodeHttpPort;
    }

    public String getClustername() {
        return clustername;
    }


    public String getMetricsTimestamp() {
        return metricsTimestamp;
    }

    public String getMetricsStream() {
        return metricsStream;
    }

    public Duration getMetricsRetention() {
        return metricsRetention;
    }

    public String getMetricsDailyIndex() {
        return metricsDailyIndex;
    }

    public String getMetricsPolicy() {
        return metricsPolicy;
    }

    public Path getNativeLibDir() {
        return nativeLibDir;
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
                if (!b.isEmpty()) {
                    b.append(", ");
                }
                b.append("writable, but it is empty");
            }
            if (b.isEmpty()) {
                // all good
                return;
            }
            throw new ValidationException("Node ID file at path " + path + " isn't " + b + ". Please specify the correct path or change the permissions");
        }
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
                return Tools.normalizeURI(httpPublishUri, httpPublishUri.getScheme(), DATANODE_DEFAULT_PORT, httpPublishUri.getPath());
            }
        }
    }

    @VisibleForTesting
    URI getDefaultHttpUri() {
        return getDefaultHttpUri("/");
    }

    private URI getDefaultHttpUri(String path) {
        final URI publishUri;
        final InetAddress inetAddress = toInetAddress(bindAddress);
        if (Tools.isWildcardInetAddress(inetAddress)) {
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
                        datanodeHttpPort,
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
                        bindAddress,
                        datanodeHttpPort,
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

    @SuppressForbidden("Deliberate invocation of DNS lookup")
    public String getHostname() {
        if (hostname != null && !hostname.isBlank()) {
            // config setting always takes precedence
            return hostname;
        }

        if (DEFAULT_BIND_ADDRESS.equals(bindAddress)) {
            // no hostname is set, bind address is to 0.0.0.0 -> return host name, the OS finds
            return Tools.getLocalCanonicalHostname();
        }

        if (InetAddresses.isInetAddress(bindAddress)) {
            // bindaddress is a real IP, resolving the hostname
            try {
                InetAddress addr = InetAddress.getByName(bindAddress);
                return addr.getHostName();
            } catch (UnknownHostException e) {
                final var hostname = Tools.getLocalCanonicalHostname();
                LOG.error("Could not resolve {} to hostname, check your DNS. Using {} instead.", bindAddress, hostname);
                return hostname;
            }
        }

        // bindaddress is configured as the hostname
        return bindAddress;
    }

    public String getNodeSearchCacheSize() {
        return searchCacheSize;
    }

    public List<String> getPathRepo() {
        return pathRepo;
    }

    public List<String> getNodeRoles() {
        return nodeRoles;
    }

    public String getOpensearchHeap() {
        return opensearchHeap;
    }
}
