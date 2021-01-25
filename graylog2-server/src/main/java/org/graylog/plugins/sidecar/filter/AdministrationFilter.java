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
package org.graylog.plugins.sidecar.filter;

import com.google.inject.name.Named;
import org.graylog.plugins.sidecar.rest.models.Sidecar;

import java.util.function.Predicate;

public interface AdministrationFilter extends Predicate<Sidecar> {
    enum Type {
        COLLECTOR, CONFIGURATION, OS, STATUS
    }

    interface Factory {
        @Named("collector") AdministrationFilter createCollectorFilter(String collectorId);
        @Named("configuration") AdministrationFilter createConfigurationFilter(String configurationId);
        @Named("os") AdministrationFilter createOsFilter(String os);
        @Named("status") AdministrationFilter createStatusFilter(int status);
    }
}
