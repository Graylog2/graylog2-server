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
import org.graylog.datanode.configuration.OpensearchKeystoreProvider;
import org.graylog.security.certutil.KeyStoreDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Map;

@Path("/certificates")
@Produces(MediaType.APPLICATION_JSON)
public class CertificatesController {

    private static final Logger log = LoggerFactory.getLogger(CertificatesController.class);

    private final DatanodeKeystore datanodeKeystore;
    private final Map<OpensearchKeystoreProvider.Store, KeyStoreDto> opensearchKeystore;

    @Inject
    public CertificatesController(DatanodeKeystore keystore, Map<OpensearchKeystoreProvider.Store, KeyStoreDto> opensearchKeystore) {
        this.datanodeKeystore = keystore;
        this.opensearchKeystore = opensearchKeystore;
    }

    @GET
    public Map<OpensearchKeystoreProvider.Store, KeyStoreDto> getCertificates() {
        Map<OpensearchKeystoreProvider.Store, KeyStoreDto> certificates = new HashMap<>();
        try {
            KeyStore keystore = datanodeKeystore.loadKeystore();
            certificates.put(OpensearchKeystoreProvider.Store.CONFIGURED, KeyStoreDto.fromKeyStore(keystore));
        } catch (DatanodeKeystoreException | KeyStoreException e) {
            log.error("Could not load datanode keystore", e);
        }
        certificates.putAll(opensearchKeystore);

        return certificates;
    }

}
