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
package org.graylog2.datanode.repositories;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AzureBlobEndpointsTest {

    @ParameterizedTest
    @CsvSource({
            "https://account.blob.core.windows.net, core.windows.net",
            "https://account.blob.core.windows.net/, core.windows.net",
            "https://account.blob.core.chinacloudapi.cn, core.chinacloudapi.cn",
            "https://ACCOUNT.blob.core.usgovcloudapi.net:443, core.usgovcloudapi.net",
            "HTTPS://account.blob.core.windows.net, core.windows.net"
    })
    void endpointsOfTheAccount(String endpoint, String suffix) {
        assertThat(AzureBlobEndpoints.endpointSuffix("account", URI.create(endpoint))).contains(suffix);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://account.blob.core.windows.net",
            "https://other.blob.core.windows.net",
            "https://account.blob.",
            "https://account.file.core.windows.net",
            "https://storage.example.com",
            "http://azurite:10000/account",
            "https://account.blob.core.windows.net:8443",
            "https://account.blob.core.windows.net/container",
            "https://user@account.blob.core.windows.net",
            "https://account.blob.core.windows.net?sv=1"
    })
    void endpointsOpenSearchCannotAddress(String endpoint) {
        assertThat(AzureBlobEndpoints.endpointSuffix("account", URI.create(endpoint))).isEmpty();
    }
}
