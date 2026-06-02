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

package org.graylog.storage.opensearch3;

import com.github.joschi.jadconfig.util.Duration;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.graylog.storage.opensearch3.testing.client.mock.ServerlessOpenSearchClient;
import org.junit.jupiter.api.Test;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Options;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfficialOpensearchClientTest {

    @Test
    void testWithTimeout() {
        OfficialOpensearchClient client = ServerlessOpenSearchClient.builder().build();
        assertThat((String) client.executeWithClientTimeout(c -> asyncCall(0), "Error getting data", Duration.milliseconds(50)))
                .isEqualTo("complete");
        assertThatThrownBy(() ->
                client.executeWithClientTimeout(c -> asyncCall(100), "Error getting data", Duration.milliseconds(50)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error getting data");
    }

    @Test
    void baseRequestConfigFieldsArePreservedWhenMergingResponseTimeout() {
        final RequestConfig baseConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(42, TimeUnit.SECONDS)
                .setExpectContinueEnabled(true)
                .build();
        final ApacheHttpClient5Options baseOptions = ApacheHttpClient5Options.DEFAULT.toBuilder()
                .setRequestConfig(baseConfig)
                .build();

        final OfficialOpensearchClient client = ServerlessOpenSearchClient.builder()
                .withBaseOptions(baseOptions)
                .build();

        final RequestConfig mergedConfig = client.clientOptionsWithTimeout(Duration.seconds(5)).getRequestConfig();
        assertThat(mergedConfig.getConnectionRequestTimeout()).isEqualTo(Timeout.ofSeconds(42));
        assertThat(mergedConfig.isExpectContinueEnabled()).isTrue();
        assertThat(mergedConfig.getResponseTimeout()).isEqualTo(Timeout.ofSeconds(5));
    }

    private CompletableFuture<String> asyncCall(long timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "complete";
        });
    }

}
