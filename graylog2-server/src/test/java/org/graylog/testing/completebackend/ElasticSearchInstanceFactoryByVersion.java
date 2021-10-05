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
package org.graylog.testing.completebackend;

import org.apache.commons.lang3.NotImplementedException;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.testcontainers.containers.Network;

import java.lang.reflect.Method;

public class ElasticSearchInstanceFactoryByVersion implements ElasticsearchInstanceFactory {
    private String version;

    @Override
    public ElasticsearchInstance create(Network network) {
        throw new NotImplementedException("Create without version not implemented with this factory.");
    }

    private ElasticsearchInstance create(final String cName, final String version, final Network network) {
        try {
            Class<?> clazz = Class.forName(cName);
            Method method = clazz.getMethod("create", String.class, Network.class);
            return (ElasticsearchInstance) method.invoke(null, version, network);
        } catch (Exception ex) {
            throw new NotImplementedException("Could not create ES6 instance.");
        }
    }

    @Override
    public ElasticsearchInstance create(String version, Network network) {
        this.version = version;
        if ("6".equals(version())) {
            return create("org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6", version, network);
        } else if ("7".equals(version())) {
            return create("org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7", version, network);
        } else {
            throw new NotImplementedException("ES version " + version + " not supported.");
        }
    }

    @Override
    public String version() {
        return version.split("\\.")[0];
    }
}
