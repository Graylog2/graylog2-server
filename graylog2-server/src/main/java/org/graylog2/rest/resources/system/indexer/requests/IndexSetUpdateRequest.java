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
package org.graylog2.rest.resources.system.indexer.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.fields.BaseIndexSetFields;
import org.graylog2.indexer.indexset.fields.FieldRestrictionsField;
import org.graylog2.indexer.indexset.fields.FieldTypeProfileField;
import org.graylog2.indexer.indexset.fields.UseLegacyRotationField;
import org.graylog2.indexer.indexset.fields.WritableField;
import org.graylog2.shared.fields.IdField;
import org.graylog2.shared.fields.TitleAndDescriptionFields;

@AutoValue
@JsonDeserialize(builder = IndexSetUpdateRequest.Builder.class)
public abstract class IndexSetUpdateRequest implements
        IdField,
        TitleAndDescriptionFields,
        BaseIndexSetFields,
        UseLegacyRotationField,
        WritableField,
        FieldTypeProfileField,
        FieldRestrictionsField {

    public IndexSetConfig toIndexSetConfig(final IndexSetConfig oldConfig) {
        return oldConfig.toBuilder()
                .title(title())
                .description(description())
                .isWritable(isWritable())
                .shards(shards())
                .replicas(replicas())
                .rotationStrategyClass(rotationStrategyClass())
                .rotationStrategyConfig(rotationStrategyConfig())
                .retentionStrategyClass(retentionStrategyClass())
                .retentionStrategyConfig(retentionStrategyConfig())
                .indexOptimizationMaxNumSegments(indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexOptimizationDisabled())
                .fieldTypeRefreshInterval(fieldTypeRefreshInterval())
                .fieldTypeProfile(fieldTypeProfile())
                .dataTieringConfig(Boolean.FALSE.equals(useLegacyRotation()) ? dataTieringConfig() : null)
                .fieldRestrictions(fieldRestrictions())
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return AutoValue_IndexSetUpdateRequest.Builder.builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements
            IdFieldBuilder<Builder>,
            TitleAndDescriptionFieldsBuilder<Builder>,
            BaseIndexSetFieldsBuilder<Builder>,
            UseLegacyRotationFieldBuilder<Builder>,
            WritableFieldBuilder<Builder>,
            FieldTypeProfileFieldBuilder<Builder>,
            FieldRestrictionsFieldBuilder<Builder> {

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_IndexSetUpdateRequest.Builder()
                    .useLegacyRotation(true);
        }

        public abstract IndexSetUpdateRequest build();
    }
}
