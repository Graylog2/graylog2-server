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


import com.google.common.base.Suppliers;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog.security.certutil.KeyStoreDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Singleton
public class OpensearchKeystoreProvider implements Provider<Map<OpensearchKeystoreProvider.Store, KeyStoreDto>> {

    public enum Store {CONFIGURED, TRUSTSTORE, HTTP, TRANSPORT}

    private static final Logger log = LoggerFactory.getLogger(OpensearchKeystoreProvider.class);

    private Supplier<OpensearchSecurityConfiguration> opensearchSecurityConfiguration;

    @Inject
    public OpensearchKeystoreProvider(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onConfigurationChangeEvent(OpensearchConfigurationChangeEvent event) {
        this.opensearchSecurityConfiguration = Suppliers.memoize(() -> event.config().opensearchSecurityConfiguration());
    }

    @Override
    public Map<Store, KeyStoreDto> get() {
        if (opensearchSecurityConfiguration == null) {
            return Map.of();
        }
        OpensearchSecurityConfiguration config = opensearchSecurityConfiguration.get();

        Map<Store, KeyStoreDto> certificates = new HashMap<>();

        certificates.put(Store.TRUSTSTORE, config.getTruststore().map(t -> {
            try {
                return KeyStoreDto.fromKeyStore(t.loadKeystore());
            } catch (Exception e) {
                log.error("Error reading truststore", e);
                return KeyStoreDto.empty();
            }
        }).orElse(KeyStoreDto.empty()));

        KeyStoreDto http = KeyStoreDto.empty();
        try {
            http = KeyStoreDto.fromKeyStore(config.getHttpCertificate().loadKeystore());
        } catch (Exception e) {
            log.error("Error reading http certificate", e);

        }
        certificates.put(Store.HTTP, http);

        KeyStoreDto transport = KeyStoreDto.empty();
        try {
            transport = KeyStoreDto.fromKeyStore(config.getTransportCertificate().loadKeystore());
        } catch (Exception e) {
            log.error("Error reading transport certificate", e);
        }
        certificates.put(Store.TRANSPORT, transport);
        return certificates;
    }

}
