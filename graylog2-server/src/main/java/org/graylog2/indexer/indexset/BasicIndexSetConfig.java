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
