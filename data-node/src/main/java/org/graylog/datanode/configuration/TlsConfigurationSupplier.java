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
package org.graylog.datanode.configuration;

import java.util.Map;

import static org.graylog.datanode.configuration.ConfigurationProvider.SSL_PREFIX;

public class TlsConfigurationSupplier {

    static final String KEYSTORE_FORMAT = "PKCS12";
    static final String TRUSTSTORE_FORMAT = "PKCS12";

    static final String TRUSTSTORE_FILENAME = "datanode-truststore.p12";

    public void addTransportTlsConfig(Map<String, String> config,
                                      final String alias,
                                      final String keystoreFile,
                                      final String keystoreFilePassword

    ) {
        final String configSubPart = SSL_PREFIX + "transport";
        addTlsConfig(config, configSubPart, alias, keystoreFile, keystoreFilePassword);
    }

    public void addHttpTlsConfig(Map<String, String> config,
                                 final String alias,
                                 final String keystoreFile,
                                 final String keystoreFilePassword) {
        final String configSubPart = SSL_PREFIX + "http";
        addTlsConfig(config, configSubPart, alias, keystoreFile, keystoreFilePassword);
        config.put(configSubPart + ".enabled", "true");
    }

    public void addTrustStoreTlsConfig(Map<String, String> config,
                                       final String truststorePassword) {
        String configSubPart = SSL_PREFIX + "http";
        config.put(configSubPart + ".truststore_type", TRUSTSTORE_FORMAT);
        config.put(configSubPart + ".truststore_filepath", TRUSTSTORE_FILENAME);
        config.put(configSubPart + ".truststore_password", truststorePassword);

        configSubPart = SSL_PREFIX + "transport";
        config.put(configSubPart + ".truststore_type", TRUSTSTORE_FORMAT);
        config.put(configSubPart + ".truststore_filepath", TRUSTSTORE_FILENAME);
        config.put(configSubPart + ".truststore_password", truststorePassword);
    }

    private void addTlsConfig(Map<String, String> config,
                              final String configPrefix,
                              final String alias,
                              final String keystoreFilePath,
                              final String keystoreFilePassword
    ) {
        config.put(configPrefix + ".keystore_type", KEYSTORE_FORMAT);
        config.put(configPrefix + ".keystore_filepath", keystoreFilePath);
        config.put(configPrefix + ".keystore_password", keystoreFilePassword);
        config.put(configPrefix + ".keystore_alias", alias);
    }
}
