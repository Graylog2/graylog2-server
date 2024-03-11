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
package org.graylog.plugins.views.search.export;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({MongoDBExtension.class, MongoJackExtension.class})
class ExportJobServiceTest {

    @Test
    void roundTrip(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider mongoJackObjectMapperProvider) {
        final SearchTypeExportJob job = SearchTypeExportJob.create(
                "000000000000000000000001",
                "000000000000000000000002",
                "000000000000000000000003",
                ResultFormat.empty());

        final ExportJobService service = new ExportJobService(mongoDBTestService.mongoConnection(),
                mongoJackObjectMapperProvider);

        assertThat(service.get(service.save(job))).isNotEmpty().containsInstanceOf(SearchTypeExportJob.class);
    }
}
