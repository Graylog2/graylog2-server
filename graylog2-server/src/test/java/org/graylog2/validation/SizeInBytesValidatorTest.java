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
package org.graylog2.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class SizeInBytesValidatorTest {

    private SizeInBytesValidator toTest;

    @BeforeEach
    void setUp() {
        toTest = new SizeInBytesValidator();
    }

    @Test
    void nullObjectIsValid() {
        assertThat(toTest.isValid(null, null)).isTrue();
    }

    @Test
    void tooShortObjectIsInvalid() {
        toTest.initialize(mockSizeInBytesAnnotation(3, 1000));
        assertThat(toTest.isValid("oh", null)).isFalse();
    }

    @Test
    void tooLongObjectIsInvalid() {
        toTest.initialize(mockSizeInBytesAnnotation(0, 7));
        assertThat(toTest.isValid("Caramba!", null)).isFalse();
    }

    @Test
    void properObjectIsValid() {
        toTest.initialize(mockSizeInBytesAnnotation(5, 10));
        assertThat(toTest.isValid("Caramba!", null)).isTrue();
    }

    @Test
    void validatesByteLengthNotStringLength() {
        toTest.initialize(mockSizeInBytesAnnotation(5, 10));
        //4 Polish letters used, increasing byte-size by 4 bytes
        assertThat(toTest.isValid("Gżegżółka", null)).isFalse();
    }

    private SizeInBytes mockSizeInBytesAnnotation(final int minValue, final int maxValue) {
        final SizeInBytes mock = mock(SizeInBytes.class);
        doReturn(minValue).when(mock).min();
        doReturn(maxValue).when(mock).max();
        return mock;
    }

}
