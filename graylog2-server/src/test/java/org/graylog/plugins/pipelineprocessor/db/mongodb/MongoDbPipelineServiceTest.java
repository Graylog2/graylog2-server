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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDbPipelineServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private MongoDbPipelineService underTest;

    @Before
    public void setup() {
        final MongoJackObjectMapperProvider mapper = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        underTest = new MongoDbPipelineService(mongodb.mongoConnection(), mapper, new ClusterEventBus());
    }

    @Test
    public void loadByRules_returnsAllPipelinesUsingProvidedRules() {
        // given
        saveNewPipelineDao("Pipeline 1", "pipeline \"Pipeline 1\"\n" +
                "stage 0 match either\n" +
                    "rule \"debug#1\"\n" +
                    "rule \"debug#2\"\n" +
                "end");

        saveNewPipelineDao("Pipeline 2", "pipeline \"Pipeline 2\"\n" +
                "stage 0 match either\n" +
                "end");

        saveNewPipelineDao("Pipeline 3", "pipeline \"Pipeline 3\"\n" +
                "stage 0 match either\n" +
                "rule \"debug#2\"\n" +
                "rule \"debug#3\"\n" +
                "end");

        saveNewPipelineDao("Pipeline 4", "pipeline \"Pipeline 4\"\n" +
                "stage 0 match either\n" +
                "rule \"debug#3\"\n" +
                "end");

        // when + then
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#3"))).satisfies(containsPipelines("Pipeline 3", "Pipeline 4"));
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#2", "debug#3"))).satisfies(containsPipelines("Pipeline 1", "Pipeline 3", "Pipeline 4"));
        assertThat(underTest.loadByRules(ImmutableSet.of())).satisfies(containsPipelines());
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#4"))).satisfies(containsPipelines());
    }

    private void saveNewPipelineDao(String title, String source) {
        underTest.save(PipelineDao.builder()
                .title(title)
                .description("Description")
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .source(source)
                .build());
    }

    private Consumer<List<? extends PipelineDao>> containsPipelines(String... pipelineTitles) {
        return pipelines -> {
            assertThat(pipelines).hasSize(pipelineTitles.length);

            for (String pt : pipelineTitles) {
                assertThat(pipelines).anySatisfy(p -> {
                    assertThat(p.title()).isEqualTo(pt);
                });
            }
        };
    }
}
