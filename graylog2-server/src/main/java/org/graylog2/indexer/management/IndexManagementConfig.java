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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;

/**
 * Legacy cluster config settings class that used when initially establishing in-database/pluggable index set
 * management in Graylog 2.0.
 * This class should no longer be referenced by new code.
 *
 * See {@link IndexSetsDefaultConfiguration} for current defaults.
 */
@Deprecated
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class IndexManagementConfig {
    @JsonProperty("rotation_strategy")
    public abstract String rotationStrategy();

    @JsonProperty("retention_strategy")
    public abstract String retentionStrategy();

    @JsonCreator
    public static IndexManagementConfig create(@JsonProperty("rotation_strategy") String rotationStrategy,
                                               @JsonProperty("retention_strategy") String retentionStrategy) {
        return new AutoValue_IndexManagementConfig(rotationStrategy, retentionStrategy);
    }
}
