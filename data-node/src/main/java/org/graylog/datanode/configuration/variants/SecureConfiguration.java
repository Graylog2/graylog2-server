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
package org.graylog.datanode.configuration.variants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import org.graylog.datanode.Configuration;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.security.hashing.BCryptPasswordAlgorithm;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public sealed abstract class SecureConfiguration implements SecurityConfigurationVariant permits MongoCertSecureConfiguration, UploadedCertFilesSecureConfiguration {

    public static final String SSL_PREFIX = "plugins.security.ssl.";

    final Path datanodeConfigDir;
    final Path opensearchConfigDir;

    public SecureConfiguration(final Configuration localConfiguration) {
        this.opensearchConfigDir = Path.of(localConfiguration.getOpensearchConfigLocation()).resolve("opensearch");
        this.datanodeConfigDir = Path.of(localConfiguration.getConfigLocation());
    }

    protected ImmutableMap<String, String> commonSecureConfig(final Configuration localConfiguration) {
        final ImmutableMap.Builder<String, String> config = ImmutableMap.builder();
        config.putAll(commonConfig(localConfiguration));

        config.put("plugins.security.disabled", "false");
        config.put(SSL_PREFIX + "http.enabled", "true");
        config.put("plugins.security.allow_default_init_securityindex", "true");
        //config.put("plugins.security.authcz.admin_dn", "CN=kirk,OU=client,O=client,L=test,C=de");

        config.put("plugins.security.audit.type", "internal_opensearch");
        config.put("plugins.security.enable_snapshot_restore_privilege", "true");
        config.put("plugins.security.check_snapshot_restore_write_privileges", "true");
        config.put("plugins.security.restapi.roles_enabled", "all_access,security_rest_api_access");
        config.put("plugins.security.system_indices.enabled", "true");
        config.put("plugins.security.system_indices.indices", ".plugins-ml-model,.plugins-ml-task,.opendistro-alerting-config,.opendistro-alerting-alert*,.opendistro-anomaly-results*,.opendistro-anomaly-detector*,.opendistro-anomaly-checkpoints,.opendistro-anomaly-detection-state,.opendistro-reports-*,.opensearch-notifications-*,.opensearch-notebooks,.opensearch-observability,.opendistro-asynchronous-search-response*,.replication-metadata-store");
        config.put("node.max_local_storage_nodes", "3");

        return config.build();
    }

    protected void configureInitialAdmin(final Configuration localConfiguration,
                                         final String adminUsername,
                                         final String adminPassword) throws IOException {
        final Path internalUsersFile = opensearchConfigDir.resolve("opensearch-security").resolve("internal_users.yml");

        Objects.requireNonNull(localConfiguration.getRestApiUsername(),
                "rest_api_username has to be configured the usage of secured Opensearch REST api"
        );

        Objects.requireNonNull(localConfiguration.getRestApiPassword(),
                "rest_api_password has to be configured the usage of secured Opensearch REST api"
        );

        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        final Map<String, Object> map = mapper.readValue(new FileInputStream(internalUsersFile.toFile()), TypeReferences.MAP_STRING_OBJECT);
        final Map<String, Object> adminUserConfig = (Map) map.get("admin");

        map.remove("admin");
        map.put(adminUsername, adminUserConfig);

        final BCryptPasswordAlgorithm passwordAlgorithm = new BCryptPasswordAlgorithm(12);
        final String hashWithPrefix = passwordAlgorithm.hash(adminPassword);

        // remove the prefix and suffix, we need just the hash itself
        final String hash = hashWithPrefix.substring("{bcrypt}".length(), hashWithPrefix.indexOf("{salt}"));
        adminUserConfig.put("hash", hash);

        final FileOutputStream fos = new FileOutputStream(internalUsersFile.toFile());
        mapper.writeValue(fos, map);
    }


}
