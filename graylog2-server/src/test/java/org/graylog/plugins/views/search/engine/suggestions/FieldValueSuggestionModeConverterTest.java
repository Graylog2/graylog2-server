package org.graylog.plugins.views.search.engine.suggestions;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldValueSuggestionModeConverterTest {

    private final Converter<FieldValueSuggestionMode> converter = new FieldValueSuggestionModeConverter();

    @Test
    void convertFrom() {
        Assertions.assertThat(converter.convertFrom("ON")).isEqualTo(FieldValueSuggestionMode.ON);
        Assertions.assertThat(converter.convertFrom("OFF")).isEqualTo(FieldValueSuggestionMode.OFF);
        Assertions.assertThat(converter.convertFrom("TEXTUAL_ONLY")).isEqualTo(FieldValueSuggestionMode.TEXTUAL_ONLY);

        // case sensitivity
        Assertions.assertThat(converter.convertFrom("on")).isEqualTo(FieldValueSuggestionMode.ON);

        // whitespace around
        Assertions.assertThat(converter.convertFrom(" on ")).isEqualTo(FieldValueSuggestionMode.ON);

        Assertions.assertThatThrownBy(() -> converter.convertFrom("nonsence"))
                .isInstanceOf(ParameterException.class)
                .hasMessageContaining("Parameter should have one of the allowed values");
    }

    @Test
    void convertTo() {
        Assertions.assertThat(converter.convertTo(FieldValueSuggestionMode.ON)).isEqualTo("ON");
    }
}
