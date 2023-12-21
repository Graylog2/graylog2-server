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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;

public class V20191219090834_AddSourcesPage extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20191219090834_AddSourcesPage.class);

    @Inject
    public V20191219090834_AddSourcesPage() {
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-12-19T09:08:34Z");
    }

    @Override
    public void upgrade() {
        LOG.debug("Migration has been replaced with new version - not doing anything.");
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("content_pack_id")
        public abstract String contentPackId();

        @JsonCreator
        public static V20191219090834_AddSourcesPage.MigrationCompleted create(@JsonProperty("content_pack_id") final String contentPackId) {
            return new AutoValue_V20191219090834_AddSourcesPage_MigrationCompleted(contentPackId);
        }
    }
}
