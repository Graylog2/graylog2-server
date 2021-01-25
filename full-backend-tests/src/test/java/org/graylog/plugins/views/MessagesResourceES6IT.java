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
package org.graylog.plugins.views;

import io.restassured.specification.RequestSpecification;
import org.graylog.storage.elasticsearch6.ElasticsearchInstanceES6Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;

import static org.graylog.testing.completebackend.Lifecycle.CLASS;

@ApiIntegrationTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticsearchInstanceES6Factory.class)
class MessagesResourceES6IT extends MessagesResourceIT {
    public MessagesResourceES6IT(GraylogBackend sut, RequestSpecification requestSpec) {
        super(sut, requestSpec);
    }
}
