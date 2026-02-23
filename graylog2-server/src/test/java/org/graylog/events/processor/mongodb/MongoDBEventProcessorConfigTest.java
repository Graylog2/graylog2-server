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
package org.graylog.events.processor.mongodb;

import org.graylog2.plugin.rest.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDBEventProcessorConfigTest {

    private static final String VALID_PIPELINE = """
            [{"$group": {"_id": null, "count": {"$sum": 1}}}]""";

    private MongoDBEventProcessorConfig validConfig() {
        return MongoDBEventProcessorConfig.builder()
                .collectionName("test_collection")
                .aggregationPipeline(VALID_PIPELINE)
                .timestampField("bucket")
                .searchWithinSeconds(60)
                .executeEverySeconds(60)
                .build();
    }

    @Test
    void isUserPresentable_returnsFalse() {
        final MongoDBEventProcessorConfig config = validConfig();
        assertThat(config.isUserPresentable()).isFalse();
    }

    @Test
    void validate_alwaysRejectsDirectUse() {
        final MongoDBEventProcessorConfig config = validConfig();
        final ValidationResult result = config.validate(null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getErrors()).containsKey("type");
    }

    @Test
    void validate_rejectsPipelineWithOutOperator() {
        final MongoDBEventProcessorConfig config = MongoDBEventProcessorConfig.builder()
                .collectionName("test_collection")
                .aggregationPipeline("[{\"$out\": \"malicious_collection\"}]")
                .timestampField("bucket")
                .searchWithinSeconds(60)
                .executeEverySeconds(60)
                .build();

        final ValidationResult result = config.validate(null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getErrors()).containsKey("aggregation_pipeline");
        assertThat(result.getErrors().get("aggregation_pipeline").toString())
                .contains("$out or $merge");
    }

    @Test
    void validate_rejectsPipelineWithMergeOperator() {
        final MongoDBEventProcessorConfig config = MongoDBEventProcessorConfig.builder()
                .collectionName("test_collection")
                .aggregationPipeline("[{\"$merge\": {\"into\": \"malicious_collection\"}}]")
                .timestampField("bucket")
                .searchWithinSeconds(60)
                .executeEverySeconds(60)
                .build();

        final ValidationResult result = config.validate(null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getErrors()).containsKey("aggregation_pipeline");
        assertThat(result.getErrors().get("aggregation_pipeline").toString())
                .contains("$out or $merge");
    }

    @Test
    void validate_validPipelineDoesNotProducePipelineErrors() {
        final MongoDBEventProcessorConfig config = validConfig();
        final ValidationResult result = config.validate(null);

        // The type error is always present, but no pipeline-specific errors
        assertThat(result.getErrors()).containsKey("type");
        assertThat(result.getErrors()).doesNotContainKey("aggregation_pipeline");
    }

    @Test
    void validate_rejectsEmptyCollectionName() {
        final MongoDBEventProcessorConfig config = MongoDBEventProcessorConfig.builder()
                .collectionName("")
                .aggregationPipeline(VALID_PIPELINE)
                .timestampField("bucket")
                .searchWithinSeconds(60)
                .executeEverySeconds(60)
                .build();

        final ValidationResult result = config.validate(null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getErrors()).containsKey("collection_name");
    }

    @Test
    void validate_rejectsEmptyPipeline() {
        final MongoDBEventProcessorConfig config = MongoDBEventProcessorConfig.builder()
                .collectionName("test_collection")
                .aggregationPipeline("")
                .timestampField("bucket")
                .searchWithinSeconds(60)
                .executeEverySeconds(60)
                .build();

        final ValidationResult result = config.validate(null);

        assertThat(result.failed()).isTrue();
        assertThat(result.getErrors()).containsKey("aggregation_pipeline");
    }
}
