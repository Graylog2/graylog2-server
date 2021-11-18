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
package org.graylog.testing.containermatrix;

import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.mongodb.MongoDBContainer;

public interface ContainerVersions {
    String DEFAULT_ES = ElasticsearchInstance.DEFAULT_VERSION;
    String DEFAULT_MONGO = MongoDBContainer.DEFAULT_VERSION;

    String ES7 = DEFAULT_ES;
    String ES6 = "6.8.4";

    String MONGO3 = "3.6";
    String MONGO4 = "4.0";
}
