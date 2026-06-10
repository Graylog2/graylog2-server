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
package org.graylog2.lookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@SuppressWarnings("ConstantConditions")
class LookupTableServiceTest {

    @Mock
    private LookupTableService service;
    @Mock
    private LookupTable table;
    private LookupTableService.Function function;

    @BeforeEach
    void setUp() {
        this.function = new LookupTableService.Function(service, "table");

        when(service.getTable("table")).thenReturn(table);
    }

    @Test
    void functionSetValue() {
        function.setValue("key", "value");

        assertThatThrownBy(() -> function.setValue("key", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setValue(null, "value2")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setValue(null, null)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).setValue("key", "value");
        verify(table, never()).setValue("key", null);
        verify(table, never()).setValue(null, "value2");
        verify(table, never()).setValue(null, null);
    }

    @Test
    void functionSetValueWithTtl() {
        function.setValueWithTtl("key", "value", 500L);

        assertThatThrownBy(() -> function.setValueWithTtl("key", null, 500L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setValueWithTtl(null, "value2", 500L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setValueWithTtl(null, null, 500L)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).setValueWithTtl("key", "value", 500L);
        verify(table, never()).setValueWithTtl("key", null, 500L);
        verify(table, never()).setValueWithTtl(null, "value2", 500L);
        verify(table, never()).setValueWithTtl(null, null, 500L);
    }

    @Test
    void functionSetStringList() {
        function.setStringList("key", List.of("hello", "world"));
        function.setStringList("key", Arrays.asList("with", "empty", "", "and", null));

        assertThatThrownBy(() -> function.setStringList("key", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setStringList(null, List.of("none"))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setStringList(null, null)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).setStringList("key", List.of("hello", "world"));
        verify(table, times(1)).setStringList("key", List.of("with", "empty", "and"));

        verify(table, never()).setStringList("key", null);
        verify(table, never()).setStringList(null, List.of("none"));
        verify(table, never()).setStringList(null, null);
        verify(table, never()).setStringList("key", Arrays.asList("with", "empty", "", "and", null));
    }

    @Test
    void functionSetStringListWithTtl() {
        function.setStringListWithTtl("key", List.of("hello", "world"), 500L);
        function.setStringListWithTtl("key", Arrays.asList("with", "empty", "", "and", null), 500L);

        assertThatThrownBy(() -> function.setStringListWithTtl("key", null, 500L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setStringListWithTtl(null, List.of("none"), 500L)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.setStringListWithTtl(null, null, 500L)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).setStringListWithTtl("key", List.of("hello", "world"), 500L);
        verify(table, times(1)).setStringListWithTtl("key", List.of("with", "empty", "and"), 500L);

        verify(table, never()).setStringListWithTtl("key", null, 500L);
        verify(table, never()).setStringListWithTtl(null, List.of("none"), 500L);
        verify(table, never()).setStringListWithTtl(null, null, 500L);
        verify(table, never()).setStringListWithTtl("key", Arrays.asList("with", "empty", "", "and", null), 500L);
    }

    @Test
    void functionAddStringList() {
        function.addStringList("key", List.of("hello", "world"), false);
        function.addStringList("key", Arrays.asList("with", "empty", "", "and", null), false);

        assertThatThrownBy(() -> function.addStringList("key", null, false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.addStringList(null, List.of("none"), false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.addStringList(null, null, false)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).addStringList("key", List.of("hello", "world"), false);
        verify(table, times(1)).addStringList("key", List.of("with", "empty", "and"), false);

        verify(table, never()).addStringList("key", null, false);
        verify(table, never()).addStringList(null, List.of("none"), false);
        verify(table, never()).addStringList(null, null, false);
        verify(table, never()).addStringList("key", Arrays.asList("with", "empty", "", "and", null), false);
    }

    @Test
    void functionRemoveStringList() {
        function.removeStringList("key", List.of("hello", "world"));
        function.removeStringList("key", Arrays.asList("with", "empty", "", "and", null));

        assertThatThrownBy(() -> function.removeStringList("key", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.removeStringList(null, List.of("none"))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> function.removeStringList(null, null)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).removeStringList("key", List.of("hello", "world"));
        verify(table, times(1)).removeStringList("key", List.of("with", "empty", "and"));

        verify(table, never()).removeStringList("key", null);
        verify(table, never()).removeStringList(null, List.of("none"));
        verify(table, never()).removeStringList(null, null);
        verify(table, never()).removeStringList("key", Arrays.asList("with", "empty", "", "and", null));
    }

    @Test
    void functionClearKey() {
        function.clearKey("key");

        assertThatThrownBy(() -> function.clearKey(null)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).clearKey("key");
        verify(table, never()).clearKey(null);
    }

    @Test
    void functionAssignTtl() {
        function.assignTtl("key", 500L);

        assertThatThrownBy(() -> function.assignTtl(null, 500L)).isInstanceOf(NullPointerException.class);

        verify(table, times(1)).assignTtl("key", 500L);
        verify(table, never()).clearKey(null);
    }
}
