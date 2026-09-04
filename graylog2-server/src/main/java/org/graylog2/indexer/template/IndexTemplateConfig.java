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
package org.graylog2.indexer.template;

import com.google.auto.value.AutoValue;
import org.graylog2.indexer.indexset.fields.CustomFieldMappingsField;
import org.graylog2.indexer.indexset.fields.FieldTypeProfileField;
import org.graylog2.indexer.indexset.fields.IndexAnalyzerField;
import org.graylog2.indexer.indexset.fields.IndexPrefixField;
import org.graylog2.indexer.indexset.fields.IndexTemplateNameField;
import org.graylog2.indexer.indexset.fields.IndexTemplateTypeField;

@AutoValue
public abstract class IndexTemplateConfig implements
        IndexPrefixField,
        IndexTemplateTypeField,
        IndexTemplateNameField,
        FieldTypeProfileField,
        CustomFieldMappingsField,
        IndexAnalyzerField {

    public abstract String indexWildcard();


    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_IndexTemplateConfig.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements
            FieldTypeProfileField.FieldTypeProfileFieldBuilder<Builder>,
            IndexTemplateTypeFieldBuilder<Builder>,
            IndexTemplateNameFieldBuilder<Builder>,
            CustomFieldMappingsField.CustomFieldMappingsFieldBuilder<Builder>,
            IndexAnalyzerField.IndexAnalyzerFieldBuilder<Builder>,
            IndexPrefixFieldBuilder<Builder> {

        public abstract Builder indexWildcard(String indexWildcard);

        public abstract IndexTemplateConfig build();
    }
}
