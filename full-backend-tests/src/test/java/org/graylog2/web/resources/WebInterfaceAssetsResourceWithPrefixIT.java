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
package org.graylog2.web.resources;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProviderWithFrontend;
import org.graylog.testing.containermatrix.annotations.FullBackendTest;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS,
                             mavenProjectDirProvider = MavenProjectDirProviderWithFrontend.class,
                             additionalConfigurationParameters = {
                                     @GraylogBackendConfiguration.ConfigurationParameter(key = "GRAYLOG_HTTP_PUBLISH_URI", value = "http://localhost:9000/graylog")
                             })
public class WebInterfaceAssetsResourceWithPrefixIT extends WebInterfaceAssetsResourceBase {

    @FullBackendTest
    void testIndexHtml() {
        testFrontend("/graylog/");
    }
}
