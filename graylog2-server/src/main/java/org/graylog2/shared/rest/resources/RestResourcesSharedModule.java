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
package org.graylog2.shared.rest.resources;

import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.rest.resources.documentation.DocumentationBrowserResource;
import org.graylog2.shared.rest.resources.documentation.DocumentationResource;
import org.graylog2.shared.rest.resources.system.LoadBalancerStatusResource;
import org.graylog2.shared.rest.resources.system.MetricsResource;
import org.graylog2.shared.rest.resources.system.SystemPluginResource;
import org.graylog2.shared.rest.resources.system.SystemResource;
import org.graylog2.shared.rest.resources.system.ThroughputResource;
import org.graylog2.shared.rest.resources.system.codecs.CodecTypesResource;
import org.graylog2.shared.rest.resources.system.inputs.InputTypesResource;

public class RestResourcesSharedModule extends Graylog2Module {
    @Override
    protected void configure() {
        addSystemRestResource(DocumentationBrowserResource.class);
        addSystemRestResource(DocumentationResource.class);
        addSystemRestResource(CodecTypesResource.class);
        addSystemRestResource(InputTypesResource.class);
        addSystemRestResource(LoadBalancerStatusResource.class);
        addSystemRestResource(MetricsResource.class);
        addSystemRestResource(SystemPluginResource.class);
        addSystemRestResource(SystemResource.class);
        addSystemRestResource(ThroughputResource.class);
    }
}
