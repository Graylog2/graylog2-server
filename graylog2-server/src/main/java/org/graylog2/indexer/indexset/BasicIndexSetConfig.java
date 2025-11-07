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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.indexer.indexset.fields.FieldTypeProfileField;
import org.graylog2.indexer.indexset.fields.IndexAnalyzerField;
import org.graylog2.indexer.indexset.fields.IndexPrefixField;
import org.graylog2.indexer.indexset.fields.IndexTemplateNameField;
import org.graylog2.indexer.indexset.fields.IndexTemplateTypeField;
import org.graylog2.indexer.indexset.fields.ShardsAndReplicasField;

public interface BasicIndexSetConfig extends
        ShardsAndReplicasField,
        IndexAnalyzerField,
        FieldTypeProfileField,
        IndexTemplateTypeField,
        IndexTemplateNameField,
        IndexPrefixField {
    String FIELD_CUSTOM_FIELD_MAPPINGS = "custom_field_mappings";


    @JsonProperty(FIELD_CUSTOM_FIELD_MAPPINGS)
    CustomFieldMappings customFieldMappings();

    interface BasicIndexSetConfigBuilder<T> extends
            ShardsAndReplicasFieldBuilder<T>,
            IndexAnalyzerFieldBuilder<T>,
            FieldTypeProfileFieldBuilder<T>,
            IndexTemplateTypeFieldBuilder<T>,
            IndexTemplateNameFieldBuilder<T>,
            IndexPrefixFieldBuilder<T> {

        @JsonProperty(FIELD_CUSTOM_FIELD_MAPPINGS)
        T customFieldMappings(CustomFieldMappings customFieldMappings);

    }
}
