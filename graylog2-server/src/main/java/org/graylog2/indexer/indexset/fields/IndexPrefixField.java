package org.graylog2.indexer.indexset.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.graylog2.validation.SizeInBytes;

public interface IndexPrefixField {
    String FIELD_INDEX_PREFIX = "index_prefix";
    String INDEX_PREFIX_REGEX = "^[a-z0-9][a-z0-9_+-]*$";

    @JsonProperty(IndexPrefixField.FIELD_INDEX_PREFIX)
    @NotBlank
    @Pattern(regexp = IndexPrefixField.INDEX_PREFIX_REGEX)
    @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
    String indexPrefix();

    interface IndexPrefixFieldBuilder<T> {

        @JsonProperty(IndexPrefixField.FIELD_INDEX_PREFIX)
        T indexPrefix(@NotBlank
                      @Pattern(regexp = IndexPrefixField.INDEX_PREFIX_REGEX)
                      @SizeInBytes(message = "Index prefix must have a length in bytes between {min} and {max}", min = 1, max = 250)
                      String indexPrefix);

    }
}
