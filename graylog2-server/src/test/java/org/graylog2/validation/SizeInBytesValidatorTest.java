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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SizeInBytesValidatorTest {

    @Mock
    SizeInBytes annotation;

    SizeInBytesValidator toTest;

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
        doReturn(3).when(annotation).min();
        toTest.initialize(annotation);
        assertThat(toTest.isValid("oh", null)).isFalse();
    }

    @Test
    void tooLongObjectIsInvalid() {
        doReturn(7).when(annotation).max();
        toTest.initialize(annotation);
        assertThat(toTest.isValid("Caramba!", null)).isFalse();
    }

    @Test
    void properObjectIsValid() {
        doReturn(10).when(annotation).max();
        doReturn(5).when(annotation).min();
        toTest.initialize(annotation);
        assertThat(toTest.isValid("Caramba!", null)).isTrue();
    }

    @Test
    void validatesByteLengthNotStringLength() {
        doReturn(10).when(annotation).max();
        doReturn(5).when(annotation).min();
        toTest.initialize(annotation);
        //4 Polish letters used, increasing byte-size by 4 bytes
        assertThat(toTest.isValid("Gżegżółka", null)).isFalse();
    }

}
