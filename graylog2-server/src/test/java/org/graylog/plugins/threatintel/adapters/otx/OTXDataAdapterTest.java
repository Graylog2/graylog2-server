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
import com.google.common.io.Resources;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.graylog2.plugin.lookup.LookupResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URL;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class OTXDataAdapterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private OTXDataAdapter otxDataAdapter;

    @Before
    public void setUp() throws Exception {
        final OTXDataAdapter.Config defaultConfiguration = new OTXDataAdapter.Descriptor().defaultConfiguration();
        final MetricRegistry metricRegistry = new MetricRegistry();

        this.otxDataAdapter = new OTXDataAdapter("1", "otx-test", defaultConfiguration, new OkHttpClient(), metricRegistry);
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
    public void isPrivateIPAddress() throws Exception {
        assertThat(otxDataAdapter.isPrivateIPAddress("0.0.0.0")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("127.0.0.1")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("192.168.1.1")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("192.168.178.56")).isTrue();
        assertThat(otxDataAdapter.isPrivateIPAddress("8.8.8.8")).isFalse();
        assertThat(otxDataAdapter.isPrivateIPAddress("137.254.56.25")).isFalse();
    }
}