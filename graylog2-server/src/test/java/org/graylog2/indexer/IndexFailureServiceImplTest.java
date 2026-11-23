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
package org.graylog2.indexer;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class IndexFailureServiceImplTest {
    private IndexFailureServiceImpl indexFailureService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        indexFailureService = new IndexFailureServiceImpl(mongodb.mongoConnection());
    }

    @Test
    void countsFailuresPerIndexSinceTheGivenTime() {
        final DateTime now = Tools.nowUTC();
        save("graylog_1", now);
        save("graylog_1", now.minusMinutes(5));
        save("graylog_2", now);
        save("graylog_1", now.minusMinutes(20));

        assertThat(indexFailureService.countByIndexSince(now.minusMinutes(10)))
                .isEqualTo(Map.of("graylog_1", 2L, "graylog_2", 1L));
    }

    private void save(String index, DateTime timestamp) {
        indexFailureService.saveWithoutValidation(new IndexFailureImpl(Map.of(
                "letter_id", "letter",
                "index", index,
                "type", "indexing",
                "message", "mapping error",
                "timestamp", timestamp)));
    }
}
