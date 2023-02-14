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
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.IntegerConverter;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import org.graylog.datanode.configuration.BaseConfiguration;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to hold configuration of Graylog
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration extends BaseConfiguration {
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
    private String opensearchLocation = "./data-node/bin/opensearch-2.4.1";

    @Parameter(value = "opensearch_version")
    private String opensearchVersion = "2.4.1";

    @Parameter(value = "opensearch_data_location")
    private String opensearchDataLocation = "./data-node/bin/data";

    @Parameter(value = "opensearch_logs_location")
    private String opensearchLogsLocation = "./data-node/bin/logs";

    @Parameter(value = "opensearch_config_location")
    private String opensearchConfigLocation = "./data-node/bin/config";

    @Parameter(value = "process_logs_buffer_size")
    private Integer logs = 500;


    @Parameter(value = "datanode_node_name")
    private String datanodeNodeName = "node1";

    @Parameter(value = "opensearch_http_port", converter = IntegerConverter.class)
    private int opensearchHttpPort = 9200;


    @Parameter(value = "opensearch_transport_port", converter = IntegerConverter.class)
    private int opensearchTransportPort = 9300;

    @Parameter(value = "opensearch_discovery_seed_hosts", converter = StringListConverter.class)
    private List<String> opensearchDiscoverySeedHosts = Collections.emptyList();

    @Parameter(value = "datanode_transport_certificate")
    private String datanodeTransportCertificate = "datanode-transport-certificates.p12";

    @Parameter(value = "datanode_transport_certificate_password")
    private String datanodeTransportCertificatePassword = null;

    @Parameter(value = "datanode_http_certificate")
    private String datanodeHttpCertificate = "datanode-http-certificates.p12";

    @Parameter(value = "datanode_http_certificate_password")
    private String datanodeHttpCertificatePassword = null;


    @Parameter(value = "datanode_admin_initial_password_hash")
    private String datanodeInitialPasswordHash = "$2y$12$hccemIPDfk5WYz0TVE9e0uRtrwgVJAkXrsUqfuIuG8.q8smnloQSO";

    @Parameter(value = "stale_leader_timeout", validators = PositiveIntegerValidator.class)
    private Integer staleLeaderTimeout = 2000;

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

    public String getOpensearchConfigLocation() {
        return opensearchConfigLocation;
    }

    public String getOpensearchLocation() {
        return opensearchLocation;
    }

    public String getOpensearchVersion() {
        return opensearchVersion;
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

    @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
    private String passwordSecret;

    @Parameter(value = "node_id_file", validators = NodeIdFileValidator.class)
    private String nodeIdFile = "/etc/graylog/server/node-id";

    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    // Required unless "root-user" is deactivated in the "deactivated_builtin_authentication_providers" setting
    @Parameter(value = "root_password_sha2")
    private String rootPasswordSha2;

    @Parameter(value = "root_timezone")
    private DateTimeZone rootTimeZone = DateTimeZone.UTC;

    @Parameter(value = "root_email")
    private String rootEmail = "";

    @Parameter(value = "user_password_default_algorithm")
    private String userPasswordDefaultAlgorithm = "bcrypt";

    @Parameter(value = "user_password_bcrypt_salt_size", validators = PositiveIntegerValidator.class)
    private int userPasswordBCryptSaltSize = 10;

    public String getPasswordSecret() {
        return passwordSecret.trim();
    }

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
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

    public String getDatanodeInitialPasswordHash() {
        return datanodeInitialPasswordHash;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validatePasswordSecret() throws ValidationException {
        final String passwordSecret = getPasswordSecret();
        if (passwordSecret == null || passwordSecret.length() < 16) {
            throw new ValidationException("The minimum length for \"password_secret\" is 16 characters.");
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateRootUser() throws ValidationException {
        if (getRootPasswordSha2() == null) {
            throw new ValidationException("Required parameter \"root_password_sha2\" not found.");
        }
    }

    public String getAdminInitialPasswordHash() {
        return null;
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
}
