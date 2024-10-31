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
