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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.fields.ExtendedIndexSetFields;
import org.graylog2.indexer.indexset.fields.FieldRestrictionsField;
import org.graylog2.indexer.indexset.fields.UseLegacyRotationField;
import org.graylog2.indexer.indexset.restrictions.IndexSetFieldRestriction;
import org.graylog2.validation.ValidObjectId;

import javax.annotation.Nullable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = IndexSetCreationRequest.Builder.class)
public abstract class IndexSetCreationRequest implements
        ExtendedIndexSetFields,
        UseLegacyRotationField,
        FieldRestrictionsField {

    private static final String INDEX_SET_TEMPLATE_ID = "index_set_template_id";

    @Nullable
    @JsonProperty(INDEX_SET_TEMPLATE_ID)
    @ValidObjectId
    public abstract String indexSetTemplateId();

    public IndexSetConfig toIndexSetConfig(boolean isRegular, Map<String, Set<IndexSetFieldRestriction>> fieldRestrictions) {
        final IndexSetConfig.Builder builder = IndexSetConfig.builder()
                .title(title())
                .description(description())
                .isWritable(isWritable())
                .isRegular(isRegular)
                .indexPrefix(indexPrefix())
                .shards(shards())
                .replicas(replicas())
                .rotationStrategyClass(rotationStrategyClass())
                .rotationStrategyConfig(rotationStrategyConfig())
                .retentionStrategyClass(retentionStrategyClass())
                .retentionStrategyConfig(retentionStrategyConfig())
                .creationDate(creationDate())
                .indexAnalyzer(indexAnalyzer())
                .indexTemplateName(indexPrefix() + "-template")
                .indexOptimizationMaxNumSegments(indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexOptimizationDisabled())
                .fieldTypeRefreshInterval(fieldTypeRefreshInterval())
                .fieldTypeProfile(fieldTypeProfile())
                .dataTieringConfig(Boolean.FALSE.equals(useLegacyRotation()) ? dataTieringConfig() : null)
                .fieldRestrictions(fieldRestrictions);

        final IndexSetConfig.Builder builderWithTemplateType = indexTemplateType().map(builder::indexTemplateType).orElse(builder);
        return builderWithTemplateType.build();
    }

    public static Builder builder() {
        return AutoValue_IndexSetCreationRequest.Builder.builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder implements
            ExtendedIndexSetFieldsBuilder<Builder>,
            UseLegacyRotationFieldBuilder<Builder>,
            FieldRestrictionsFieldBuilder<Builder> {

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_IndexSetCreationRequest.Builder()
                    .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                    .useLegacyRotation(true);
        }

        @JsonProperty(INDEX_SET_TEMPLATE_ID)
        public abstract Builder indexSetTemplateId(@Nullable @ValidObjectId String indexSetTemplateId);

        public abstract IndexSetCreationRequest build();
    }
}
