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
import io.jsonwebtoken.lang.Collections;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog.security.certutil.KeyStoreDto;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class OpensearchKeystoreProvider implements Provider<Map<OpensearchKeystoreProvider.Store, KeyStoreDto>> {

    public enum Store {CONFIGURED, TRUSTSTORE, HTTP, TRANSPORT}

    private static final Logger log = LoggerFactory.getLogger(OpensearchKeystoreProvider.class);

    private final Map<Store, KeyStoreDto> keystores = new ConcurrentHashMap<>();

    @Inject
    public OpensearchKeystoreProvider(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onConfigurationChangeEvent(OpensearchConfigurationChangeEvent event) {
        try {
            keystores.put(Store.TRUSTSTORE, KeyStoreDto.fromKeyStore(event.config().trustStore()));

            event.config().httpCertificate()
                    .map(OpensearchKeystoreProvider::toDto)
                    .ifPresentOrElse(dto -> keystores.put(Store.HTTP, dto), () -> keystores.remove(Store.HTTP));

            event.config().transportCertificate()
                    .map(OpensearchKeystoreProvider::toDto)
                    .ifPresentOrElse(dto -> keystores.put(Store.TRANSPORT, dto), () -> keystores.remove(Store.TRANSPORT));

        } catch (Exception e) {
            log.error("Error reading truststore", e);
        }
    }

    @Nonnull
    private static KeyStoreDto toDto(KeystoreInformation cert) {
        try {
            return KeyStoreDto.fromKeyStore(cert.loadKeystore());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<Store, KeyStoreDto> get() {
        return Collections.immutable(keystores);
    }

}
