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

import com.google.common.collect.ImmutableMap;
import org.graylog.datanode.configuration.certificates.CertificateMetaData;

import static org.graylog.datanode.configuration.variants.SecureConfiguration.SSL_PREFIX;

public class TlsConfigurationSupplier {

    static final String KEYSTORE_FORMAT = "PKCS12";
    static final String TRUSTSTORE_FORMAT = "PKCS12";

    public static final String TRUSTSTORE_FILENAME = "datanode-truststore.p12";

    public ImmutableMap<String, String> getTransportTlsConfig(final CertificateMetaData certificateMetaData) {
        final String configSubPart = SSL_PREFIX + "transport";
        return getTlsConfig(configSubPart, certificateMetaData);
    }

    public ImmutableMap<String, String> getHttpTlsConfig(final CertificateMetaData certificateMetaData) {
        final String configSubPart = SSL_PREFIX + "http";
        return ImmutableMap.<String, String>builder()
                .putAll(getTlsConfig(configSubPart, certificateMetaData))
                .put(configSubPart + ".enabled", "true")
                .build();
    }

    public ImmutableMap<String, String> getTrustStoreTlsConfig(final String truststorePassword) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        String configSubPart = SSL_PREFIX + "http";
        builder.put(configSubPart + ".truststore_type", TRUSTSTORE_FORMAT);
        builder.put(configSubPart + ".truststore_filepath", TRUSTSTORE_FILENAME);
        builder.put(configSubPart + ".truststore_password", truststorePassword);

        configSubPart = SSL_PREFIX + "transport";
        builder.put(configSubPart + ".truststore_type", TRUSTSTORE_FORMAT);
        builder.put(configSubPart + ".truststore_filepath", TRUSTSTORE_FILENAME);
        builder.put(configSubPart + ".truststore_password", truststorePassword);
        return builder.build();
    }

    private ImmutableMap<String, String> getTlsConfig(final String configPrefix,
                                                      final CertificateMetaData certificateMetaData
    ) {
        return ImmutableMap.<String, String>builder()
                .put(configPrefix + ".keystore_type", KEYSTORE_FORMAT)
                .put(configPrefix + ".keystore_filepath", certificateMetaData.keystoreFilePath())
                .put(configPrefix + ".keystore_password", certificateMetaData.passwordAsString())
                .put(configPrefix + ".keystore_alias", certificateMetaData.alias())
                .build();
    }
}
