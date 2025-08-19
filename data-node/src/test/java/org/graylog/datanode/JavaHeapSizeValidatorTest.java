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
package org.graylog.datanode;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JavaHeapSizeValidatorTest {

    private final Validator<String> validator = new JavaHeapSizeValidator();

    @Test
    void testInvalidValue() {
        Assertions.assertThatThrownBy(() -> validator.validate("opensearch_heap", "4GB"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid heap size configuration: 4GB. Set opensearch_heap to <size>[g|G|m|M|k|K]. For example 4g or 512m");
    }

    @Test
    void testValidValue() {
        Assertions.assertThatNoException().isThrownBy(() -> validator.validate("opensearch_heap", "7g"));
        Assertions.assertThatNoException().isThrownBy(() -> validator.validate("opensearch_heap", "512M"));
    }
}
