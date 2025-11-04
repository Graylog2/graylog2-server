package org.graylog2.indexer.indexset.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public interface IndexTemplateNameField {
    String FIELD_INDEX_TEMPLATE_NAME = "index_template_name";

    @JsonProperty(FIELD_INDEX_TEMPLATE_NAME)
    @NotBlank
    String indexTemplateName();

    interface IndexTemplateNameFieldBuilder<T> {

        @JsonProperty(IndexTemplateNameField.FIELD_INDEX_TEMPLATE_NAME)
        T indexTemplateName(String templateName);

    }
}
