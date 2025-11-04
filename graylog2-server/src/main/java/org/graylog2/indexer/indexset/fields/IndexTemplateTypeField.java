package org.graylog2.indexer.indexset.fields;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.Optional;

public interface IndexTemplateTypeField {
    String FIELD_INDEX_TEMPLATE_TYPE = "index_template_type";

    @JsonProperty(IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE)
    Optional<String> indexTemplateType();

    interface IndexTemplateTypeFieldBuilder<T> {

        @JsonProperty(IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE)
        T indexTemplateType(@Nullable String templateType);
    }
}
