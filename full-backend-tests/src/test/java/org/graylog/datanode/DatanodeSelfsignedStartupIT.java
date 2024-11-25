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

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.RetryException;
import io.restassured.response.ValidatableResponse;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.restoperations.DatanodeOpensearchWait;
import org.graylog.testing.restoperations.RestOperationParameters;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.security.JwtSecret;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV,
                                   additionalConfigurationParameters = {
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"),
                                           @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),
                                   })
public class DatanodeSelfsignedStartupIT {


    private final Logger log = LoggerFactory.getLogger(DatanodeProvisioningIT.class);

    private final GraylogApis apis;

    public DatanodeSelfsignedStartupIT(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    public void testSelfsignedStartup() throws ExecutionException, RetryException {
        testEncryptedConnectionToOpensearch();
    }


    private int getOpensearchPort() {
        final String indexerHostAddress = apis.backend().searchServerInstance().getHttpHostAddress();
        return Integer.parseInt(indexerHostAddress.split(":")[1]);
    }

    private void testEncryptedConnectionToOpensearch() throws ExecutionException, RetryException {
        try {
            final ValidatableResponse response = new DatanodeOpensearchWait(RestOperationParameters.builder()
                    .port(getOpensearchPort())
                    .relaxedHTTPSValidation(true)
                    .jwtTokenProvider(new IndexerJwtAuthTokenProvider(new JwtSecret(ContainerizedGraylogBackend.PASSWORD_SECRET), Duration.seconds(120), Duration.seconds(60)))
                    .build())
                    .waitForNodesCount(1);

            response.assertThat().body("status", Matchers.equalTo("green"));
        } catch (Exception e) {
            log.error("Could not connect to Opensearch\n" + apis.backend().getSearchLogs());
            throw e;
        }
    }
}
