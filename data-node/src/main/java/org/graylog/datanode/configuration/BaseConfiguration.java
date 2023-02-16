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

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.configuration.PathConfiguration;

public class BaseConfiguration extends PathConfiguration {
    @Parameter(value = "async_eventbus_processors")
    private int asyncEventbusProcessors = 2;

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }

}
