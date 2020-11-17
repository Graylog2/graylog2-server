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
package org.graylog.freeenterprise;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import org.graylog2.plugin.PluginConfigBean;

import java.net.URI;

public class FreeEnterpriseConfiguration implements PluginConfigBean {
    private static final String PREFIX = "free_enterprise_";

    public static final String SERVICE_URL = PREFIX + "service_url";

    @Parameter(value = SERVICE_URL, validators = URIAbsoluteValidator.class)
    private URI serviceUrl = URI.create("https://api.graylog.com/");

    public URI getServiceUrl() {
        return serviceUrl;
    }
}
