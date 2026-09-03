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
package org.graylog.plugins.threatintel.adapters.otx;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.graylog2.lookup.adapters.LookupDataAdapterValidationContext;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.system.urlallowlist.UrlAllowlistNotificationService;
import org.graylog2.system.urlallowlist.UrlAllowlistService;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URL;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class OTXDataAdapterTest {

    @Mock
    private UrlAllowlistService urlAllowlistService;

    @Mock
    private UrlAllowlistNotificationService urlAllowlistNotificationService;

    @Mock
    private LookupDataAdapterValidationContext validationContext;

    private OTXDataAdapter otxDataAdapter;

    @BeforeEach
    public void setUp() throws Exception {
        final OTXDataAdapter.Config defaultConfiguration = new OTXDataAdapter.Descriptor(CustomizationConfig.empty()).defaultConfiguration();
        final MetricRegistry metricRegistry = new MetricRegistry();

        this.otxDataAdapter = new OTXDataAdapter("1", "otx-test", defaultConfiguration, new OkHttpClient(),
                urlAllowlistService, urlAllowlistNotificationService, metricRegistry);
    }

    @Test
    public void parseResponse() throws Exception {
        final URL url = Resources.getResource(getClass(), "otx-IPv4-response.json");
        final ResponseBody body = ResponseBody.create(null, Resources.toByteArray(url));
        final LookupResult result = otxDataAdapter.parseResponse(body);

        assertThat(result.singleValue()).isEqualTo(0L);
        assertThat(result.multiValue()).isNotNull();
        assertThat(requireNonNull(result.multiValue()).get("country_name")).isEqualTo("Ireland");
    }

    @Test
    public void isPrivateIPAddress() {
        assertThat(otxDataAdapter.isPrivateIPAddress("0.0.0.0")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("127.0.0.1")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("192.168.1.1")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("192.168.178.56")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("8.8.8.8")).isFalse();
        assertThat(otxDataAdapter.isPrivateIPAddress("137.254.56.25")).isFalse();
    }

    @Test
    public void doGet_returnsErrorWhenUrlNotAllowlisted() throws Exception {
        otxDataAdapter.doStart();
        when(urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).thenReturn(false);

        final LookupResult result = otxDataAdapter.doGet("8.8.8.8");

        assertThat(result.hasError()).isTrue();
        verify(urlAllowlistNotificationService).publishAllowlistFailure(anyString());
    }

    @Test
    public void doGet_doesNotPublishNotificationWhenUrlIsAllowlisted() throws Exception {
        otxDataAdapter.doStart();
        when(urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).thenReturn(true);

        // The actual HTTP request will fail with an IOException (no server to connect to),
        // but the allowlist check passes and no allowlist failure notification is published.
        otxDataAdapter.doGet("8.8.8.8");

        verify(urlAllowlistNotificationService, never()).publishAllowlistFailure(anyString());
    }

    @Test
    public void validate_returnsErrorWhenUrlNotAllowlisted() {
        when(validationContext.getUrlAllowlistService()).thenReturn(urlAllowlistService);
        when(urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).thenReturn(false);

        final OTXDataAdapter.Config config = new OTXDataAdapter.Descriptor(CustomizationConfig.empty()).defaultConfiguration();
        final Optional<Multimap<String, String>> result = config.validate(validationContext);

        assertThat(result).isPresent();
        assertThat(result.get().containsKey("api_url")).isTrue();
    }

    @Test
    public void validate_returnsNoErrorWhenUrlIsAllowlisted() {
        when(validationContext.getUrlAllowlistService()).thenReturn(urlAllowlistService);
        when(urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).thenReturn(true);

        final OTXDataAdapter.Config config = new OTXDataAdapter.Descriptor(CustomizationConfig.empty()).defaultConfiguration();
        final Optional<Multimap<String, String>> result = config.validate(validationContext);

        assertThat(result).isEmpty();
    }
}
