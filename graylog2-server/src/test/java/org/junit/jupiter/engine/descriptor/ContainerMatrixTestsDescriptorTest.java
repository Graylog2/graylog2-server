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
package org.junit.jupiter.engine.descriptor;

import com.github.zafarkhaja.semver.Version;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.Test;

class ContainerMatrixTestsDescriptorTest {

    @Test
    void createKey() {
        final String key = ContainerMatrixTestsDescriptor.createKey(Lifecycle.CLASS, "mavenDir", "jarDir", SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.2.3")), MongodbServer.MONGO5, true);
        Assertions.assertThat(key).isEqualTo("Lifecycle: CLASS, MavenProjectDirProvider: mavenDir, PluginJarsProvider: jarDir, Search: OpenSearch:1.2.3, MongoDB: 4.0, Mailserver: enabled");
    }
}
