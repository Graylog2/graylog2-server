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
package org.graylog.storage.elasticsearch7;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParsedElasticsearchExceptionTest {
    @Test
    void parsingMapperParsingException() {
        final String exception = "ElasticsearchException[Elasticsearch exception [type=mapper_parsing_exception, " +
                "reason=failed to parse field [_ourcustomfield] of type [long] in document with id '2f1b81f1-c050-11ea-ad64-d2850321fca4'. " +
                "Preview of field's value: 'fourty-two']]; nested: ElasticsearchException[Elasticsearch exception " +
                "[type=illegal_argument_exception, reason=For input string: \"fourty-two\"]];";

        final ParsedElasticsearchException parsed = ParsedElasticsearchException.from(exception);

        assertThat(parsed).satisfies(p -> {
            assertThat(p.type()).isEqualTo("mapper_parsing_exception");
            assertThat(p.reason()).isEqualTo("failed to parse field [_ourcustomfield] of type [long] in document with " +
                    "id '2f1b81f1-c050-11ea-ad64-d2850321fca4'. Preview of field's value: 'fourty-two'");
        });
    }

    @Test
    void parsingIndexReadonlyException() {
        final String exception = "Elasticsearch exception: ElasticsearchException[Elasticsearch exception " +
                "[type=cluster_block_exception, reason=index [messages_it_deflector] blocked by: [TOO_MANY_REQUESTS/12/index " +
                "read-only / allow delete (api)];]]";

        final ParsedElasticsearchException parsed = ParsedElasticsearchException.from(exception);

        assertThat(parsed).satisfies(p -> {
            assertThat(p.type()).isEqualTo("cluster_block_exception");
            assertThat(p.reason()).isEqualTo("index [messages_it_deflector] blocked by: [TOO_MANY_REQUESTS/12/index read-only / allow delete (api)");
        });
    }

    @Test
    void parsingPrimaryShardIsNotAvailable() {
        final String exception = "Elasticsearch exception: ElasticsearchException[Elasticsearch exception [type=unavailable_shards_exception, " +
                "reason=[graylog_0][2] primary shard is not active Timeout: [1m], request: [BulkShardRequest [[graylog_0][2]] containing [125] " +
                "requests]]]";

        final ParsedElasticsearchException parsed = ParsedElasticsearchException.from(exception);

        assertThat(parsed).satisfies(p -> {
            assertThat(p.type()).isEqualTo("unavailable_shards_exception");
            assertThat(p.reason()).isEqualTo("[graylog_0][2] primary shard is not active Timeout: [1m], " +
                    "request: [BulkShardRequest [[graylog_0][2]] containing [125] requests]]");
        });
    }

    @Test
    void parsingInvalidWriteTargetMessage() {
        final String exception = "Elasticsearch exception [type=illegal_argument_exception, reason=no write index is defined for alias [messages_it_deflector]. The write index may be explicitly disabled using is_write_index=false or the alias points to multiple indices without one being designated as a write index]";

        final ParsedElasticsearchException parsed = ParsedElasticsearchException.from(exception);

        assertThat(parsed).satisfies(p -> {
            assertThat(p.type()).isEqualTo("illegal_argument_exception");
            assertThat(p.reason()).isEqualTo("no write index is defined for alias [messages_it_deflector]. " +
                    "The write index may be explicitly disabled using is_write_index=false or the alias points to " +
                    "multiple indices without one being designated as a write index");
        });
    }
}
