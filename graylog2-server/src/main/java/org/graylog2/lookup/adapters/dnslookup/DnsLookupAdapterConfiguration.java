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
package org.graylog2.lookup.adapters.dnslookup;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.graylog2.plugin.PluginConfigBean;

public class DnsLookupAdapterConfiguration implements PluginConfigBean {
    private static final String PREFIX = "dns_lookup_adapter_";
    protected static final String RESOLVER_POOL_SIZE = PREFIX + "resolver_pool_size";
    protected static final String RESOLVER_POOL_REFRESH_INTERVAL = PREFIX + "resolver_pool_refresh_interval";

    protected static final int DEFAULT_POOL_SIZE = 10;
    protected static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 300;

    @Parameter(value = RESOLVER_POOL_SIZE, validators = PositiveIntegerValidator.class)
    private int poolSize = DEFAULT_POOL_SIZE;

    @Parameter(value = RESOLVER_POOL_REFRESH_INTERVAL, validators = PositiveDurationValidator.class)
    private Duration poolRefreshInterval = Duration.seconds(DEFAULT_REFRESH_INTERVAL_SECONDS);

    public int getPoolSize() {
        return poolSize;
    }

    public Duration getPoolRefreshInterval() {
        return poolRefreshInterval;
    }
}
