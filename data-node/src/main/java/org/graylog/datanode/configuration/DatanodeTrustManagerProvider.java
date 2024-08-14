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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.TrustManagerAggregator;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;

@Singleton
public class DatanodeTrustManagerProvider implements Provider<X509TrustManager> {

    private final CustomCAX509TrustManager customCAX509TrustManager;
    private volatile KeyStore datanodeTruststore;

    @Inject
    public DatanodeTrustManagerProvider(CustomCAX509TrustManager CustomCAX509TrustManager, EventBus eventBus) {
        customCAX509TrustManager = CustomCAX509TrustManager;
        eventBus.register(this);
    }

    @Subscribe
    public void onOpensearchConfigurationChange(OpensearchConfigurationChangeEvent e) {
        Optional.ofNullable(e.config().opensearchSecurityConfiguration())
                .flatMap(OpensearchSecurityConfiguration::getTruststore)
                .map(t -> {
                    try {
                        return t.loadKeystore();
                    } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .ifPresent(this::setTruststore);
    }

    private void setTruststore(KeyStore keyStore) {
        this.datanodeTruststore = keyStore;
    }


    @Override
    public X509TrustManager get() {
        final X509TrustManager datanodeTrustManager = TrustManagerAggregator.trustManagerFromKeystore(this.datanodeTruststore);
        return new TrustManagerAggregator(List.of(datanodeTrustManager, customCAX509TrustManager));
    }
}
