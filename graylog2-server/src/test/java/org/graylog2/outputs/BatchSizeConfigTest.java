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
package org.graylog2.outputs;

import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.util.Size;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchSizeConfigTest {
    @Test
    void convertNumber() {
        final var config = new BatchSizeConfig.Converter().convertFrom("500");
        assertThat(config).isEqualTo(BatchSizeConfig.forCount(500));
        assertThat(config.getAsCount()).contains(500);
        assertThat(config.getAsBytes()).isEmpty();
    }

    @Test
    void convertSize() {
        final var config = new BatchSizeConfig.Converter().convertFrom("500 mb");
        assertThat(config).isEqualTo(BatchSizeConfig.parse("500 mb"));
        assertThat(config.getAsCount()).isEmpty();
        assertThat(config.getAsBytes()).contains(Size.megabytes(500));
    }

    @Test
    void convertInvalidValue() {
        final var converter = new BatchSizeConfig.Converter();
        assertThatThrownBy(() -> converter.convertFrom("500 exobytes")).isInstanceOf(ParameterException.class)
                .cause().hasMessageContaining("Wrong size unit");
    }

    @Test
    void validateNumber() throws ValidationException {
        final var validator = new BatchSizeConfig.Validator();
        validator.validate("setting", BatchSizeConfig.parse("500"));
        assertThatThrownBy(() ->
                validator.validate("setting", BatchSizeConfig.forCount(-1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validateSize() throws ValidationException {
        final var validator = new BatchSizeConfig.Validator();
        validator.validate("setting", BatchSizeConfig.parse("500 kb"));
        assertThatThrownBy(() ->
                validator.validate("setting", BatchSizeConfig.parse("500 mb")))
                .isInstanceOf(ValidationException.class);
    }
}
