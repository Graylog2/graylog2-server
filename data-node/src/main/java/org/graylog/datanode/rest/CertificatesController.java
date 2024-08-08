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
package org.graylog.datanode.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.graylog.datanode.configuration.DatanodeKeystore;
import org.graylog.datanode.configuration.DatanodeKeystoreException;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.security.certutil.KeyStoreDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/certificates")
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesController {

    private static final Logger log = LoggerFactory.getLogger(CertificatesController.class);

    enum Store {CONFIGURED, TRUSTSTORE, HTTP, TRANSPORT}

    private final DatanodeKeystore datanodeKeystore;
    private final OpensearchProcess opensearchProcess;

    @Inject
    public CertificatesController(DatanodeKeystore keystore, OpensearchProcess opensearchProcess) {
        this.datanodeKeystore = keystore;
        this.opensearchProcess = opensearchProcess;
    }

    @GET
    public Map<Store, KeyStoreDto> getCertificates() {
        Map<Store, KeyStoreDto> certificates = new HashMap<>();
        try {
            KeyStore keystore = datanodeKeystore.loadKeystore();
            certificates.put(Store.CONFIGURED, KeyStoreDto.fromKeyStore(keystore));
        } catch (DatanodeKeystoreException | KeyStoreException e) {
            log.error("Could not load datanode keystore", e);
        }
        Optional<OpensearchSecurityConfiguration> opensearchSecurityConfiguration = opensearchProcess.getOpensearchConfiguration()
                .map(OpensearchConfiguration::opensearchSecurityConfiguration);
        certificates.put(Store.TRUSTSTORE, opensearchSecurityConfiguration
                .flatMap(OpensearchSecurityConfiguration::getTruststore).map(t -> {
                    try {
                        return KeyStoreDto.fromKeyStore(t.loadKeystore());
                    } catch (Exception e) {
                        log.error("Error reading truststore", e);
                        return KeyStoreDto.empty();
                    }
                }).orElse(KeyStoreDto.empty()));
        certificates.put(Store.HTTP, opensearchSecurityConfiguration
                .map(OpensearchSecurityConfiguration::getHttpCertificate).map(t -> {
                    try {
                        return KeyStoreDto.fromKeyStore(t.loadKeystore());
                    } catch (Exception e) {
                        log.error("Error reading http certificate", e);
                        return KeyStoreDto.empty();
                    }
                }).orElse(KeyStoreDto.empty()));
        certificates.put(Store.TRANSPORT, opensearchSecurityConfiguration
                .map(OpensearchSecurityConfiguration::getTransportCertificate).map(t -> {
                    try {
                        return KeyStoreDto.fromKeyStore(t.loadKeystore());
                    } catch (Exception e) {
                        log.error("Error reading transport certificate", e);
                        return KeyStoreDto.empty();
                    }
                }).orElse(KeyStoreDto.empty()));
        return certificates;
    }

}
