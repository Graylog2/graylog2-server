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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.fields.ExtendedIndexSetFields;
import org.graylog2.indexer.indexset.fields.FieldRestrictionsField;
import org.graylog2.indexer.indexset.fields.UseLegacyRotationField;
import org.graylog2.shared.fields.IdField;

import javax.annotation.Nullable;

@AutoValue
public abstract class IndexSetResponse implements
        IdField,
        ExtendedIndexSetFields,
        UseLegacyRotationField,
        FieldRestrictionsField {

    public static final String FIELD_DEFAULT = "default";
    public static final String FIELD_CAN_BE_DEFAULT = "can_be_default";
    public static final String FIELD_DATA_TIERING_STATUS = "data_tiering_status";


    @JsonProperty(FIELD_DEFAULT)
    public abstract boolean isDefault();

    @JsonProperty(FIELD_CAN_BE_DEFAULT)
    public abstract boolean canBeDefault();

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(FIELD_DATA_TIERING_STATUS)
    public abstract DataTieringStatus dataTieringStatus();

    public static Builder builder() {
        return new AutoValue_IndexSetResponse.Builder();
    }

    public static IndexSetResponse fromIndexSetConfig(IndexSetConfig indexSetConfig,
                                                      boolean isDefault,
                                                      DataTieringStatus dataTieringStatus) {
        return builder()
                .id(indexSetConfig.id())
                .title(indexSetConfig.title())
                .description(indexSetConfig.description())
                .isDefault(isDefault)
                .isWritable(indexSetConfig.isWritable())
                .canBeDefault(indexSetConfig.isRegularIndex())
                .indexPrefix(indexSetConfig.indexPrefix())
                .shards(indexSetConfig.shards())
                .replicas(indexSetConfig.replicas())
                .rotationStrategyClass(indexSetConfig.rotationStrategyClass())
                .rotationStrategyConfig(indexSetConfig.rotationStrategyConfig())
                .retentionStrategyClass(indexSetConfig.retentionStrategyClass())
                .retentionStrategyConfig(indexSetConfig.retentionStrategyConfig())
                .creationDate(indexSetConfig.creationDate())
                .indexAnalyzer(indexSetConfig.indexAnalyzer())
                .indexOptimizationMaxNumSegments(indexSetConfig.indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexSetConfig.indexOptimizationDisabled())
                .fieldTypeRefreshInterval(indexSetConfig.fieldTypeRefreshInterval())
                .indexTemplateType(indexSetConfig.indexTemplateType().orElse(null))
                .fieldTypeProfile(indexSetConfig.fieldTypeProfile())
                .dataTieringConfig(indexSetConfig.dataTieringConfig())
                .useLegacyRotation(indexSetConfig.dataTieringConfig() == null)
                .dataTieringStatus(dataTieringStatus)
                .fieldRestrictions(indexSetConfig.fieldRestrictions())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder implements
            IdFieldBuilder<Builder>,
            ExtendedIndexSetFieldsBuilder<Builder>,
            UseLegacyRotationFieldBuilder<Builder>,
            FieldRestrictionsFieldBuilder<Builder> {

        public abstract Builder isDefault(boolean isDefault);

        public abstract Builder canBeDefault(boolean canBeDefault);

        public abstract Builder dataTieringStatus(@Nullable DataTieringStatus dataTieringStatus);

        public abstract IndexSetResponse build();
    }
}
