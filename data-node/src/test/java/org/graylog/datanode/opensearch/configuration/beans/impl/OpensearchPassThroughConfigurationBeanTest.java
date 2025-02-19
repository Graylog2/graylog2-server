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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

class OpensearchPassThroughConfigurationBeanTest {

    @Test
    void testPassThroughConfigurationPart() throws ValidationException, RepositoryException {
        final Configuration datanodeConfiguration = DatanodeTestUtils.datanodeConfiguration(Map.of(
                "opensearch.cluster.routing.allocation.disk.watermark.low", "98%"
        ));
        final OpensearchPassThroughConfigurationBean cb = new OpensearchPassThroughConfigurationBean(datanodeConfiguration, Collections::emptyMap);
        final DatanodeConfigurationPart configurationPart = cb.buildConfigurationPart(emptyBuildParams());

        Assertions.assertThat(configurationPart.properties())
                .hasSize(1)
                .containsEntry("cluster.routing.allocation.disk.watermark.low", "98%");

    }

    private OpensearchConfigurationParams emptyBuildParams() {
        return new OpensearchConfigurationParams(Collections.emptyList(), Collections.emptyMap());
    }
}
