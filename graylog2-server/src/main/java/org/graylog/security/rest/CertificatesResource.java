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
package org.graylog.security.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.certutil.CaTruststore;
import org.graylog.security.certutil.KeyStoreDto;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Path("/certificates")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@Api(value = "Certificates", description = "Information about certificates used")
public class CertificatesResource {
    private final Logger log = LoggerFactory.getLogger(CertificatesResource.class);

    enum Store {CONFIGURED_CA, TRUSTSTORE, HTTP}

    private final CaTruststore caTruststore;
    private final HttpConfiguration httpConfiguration;
    private final CustomCAX509TrustManager trustManager;

    @Inject
    public CertificatesResource(CaTruststore caTruststore, HttpConfiguration httpConfiguration, CustomCAX509TrustManager trustManager) {
        this.caTruststore = caTruststore;
        this.httpConfiguration = httpConfiguration;
        this.trustManager = trustManager;
    }

    @GET
    @ApiOperation("Returns the certificates used by this node")
    @RequiresPermissions(RestPermissions.GRAYLOG_CA_READ)
    public Map<Store, KeyStoreDto> getCertificates() {
        Map<Store, KeyStoreDto> certificates = new HashMap<>();
        caTruststore.getTrustStore().ifPresent(truststore -> {
            try {
                certificates.put(Store.CONFIGURED_CA, KeyStoreDto.fromKeyStore(truststore));
            } catch (KeyStoreException e) {
                log.error("Error reading truststore", e);
            }
        });

        if (httpConfiguration.isHttpEnableTls()) {
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                try (InputStream inStream = new java.io.ByteArrayInputStream(Files.readAllBytes(httpConfiguration.getHttpTlsCertFile()))) {
                    certificates.put(Store.HTTP, KeyStoreDto.fromSingleCertificate("tlscert", (X509Certificate) certFactory.generateCertificate(inStream)));
                }
            } catch (CertificateException | IOException e) {
                log.error("Error reading http tls certificate", e);
            }
        }

        certificates.put(Store.TRUSTSTORE, KeyStoreDto.fromCertificates("trustManager", trustManager.getAcceptedIssuers()));

        return certificates;
    }

}
