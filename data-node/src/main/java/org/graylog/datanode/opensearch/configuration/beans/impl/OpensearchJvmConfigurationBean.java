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

import jakarta.inject.Inject;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationBean;
import org.graylog.datanode.opensearch.configuration.beans.OpensearchConfigurationPart;

import java.security.cert.X509Certificate;
import java.util.List;

public class OpensearchJvmConfigurationBean implements OpensearchConfigurationBean {

    private final String opensearchHeap;

    @Inject
    public OpensearchJvmConfigurationBean(DatanodeConfiguration datanodeConfiguration) {
        opensearchHeap = datanodeConfiguration.opensearchHeap();
    }

    @Override
    public OpensearchConfigurationPart buildConfigurationPart(List<X509Certificate> trustedCertificates) {
        return OpensearchConfigurationPart.builder()
                .javaOpt("-Xms%s".formatted(opensearchHeap))
                .javaOpt("-Xmx%s".formatted(opensearchHeap))
                .javaOpt("-Dopensearch.transport.cname_in_publish_address=true")
                .build();
    }
}
