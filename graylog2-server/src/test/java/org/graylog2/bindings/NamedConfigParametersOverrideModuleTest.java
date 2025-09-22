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
package org.graylog2.bindings;

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.junit.Test;

import java.util.List;

import static com.google.inject.name.Names.named;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NamedConfigParametersOverrideModuleTest {
    static class BaseConfig {
        @Parameter(value = "test_param", required = true)
        private String testParam;

        BaseConfig(String testParam) {
            this.testParam = testParam;
        }
    }

    static class SubConfigA extends BaseConfig {
        SubConfigA(String testParam) {
            super(testParam);
        }
    }

    static class SubConfigB extends SubConfigA {
        @Parameter(value = "test_param_2", required = true)
        private String testParam2;

        SubConfigB(String testParam, String testParam2) {
            super(testParam);
            this.testParam2 = testParam2;
        }
    }

    static class OtherConfig {
        @Parameter(value = "test_param", required = true)
        private String testParam;

        OtherConfig(String testParam) {
            this.testParam = testParam;
        }
    }

    @Test
    public void inheritedParameterBindsOnce() {
        var subConfigA = new SubConfigA("test_param_value");
        var subConfigB = new SubConfigB("test_param_value_2", "test_param_2_value");

        Injector injector = Guice.createInjector(
                new NamedConfigParametersOverrideModule(List.of(subConfigA, subConfigB), false)
        );

        var testParam = injector.getInstance(Key.get(String.class, named("test_param")));
        var testParam2 = injector.getInstance(Key.get(String.class, named("test_param_2")));

        assertEquals("test_param_value", testParam);
        assertEquals("test_param_2_value", testParam2);
    }

    @Test
    public void duplicateParameterThrowsException() {
        var subConfigA = new SubConfigA("test_param_value");
        var otherConfig = new OtherConfig("test_param_value_2");

        assertThrows(CreationException.class, () -> Guice.createInjector(
                new NamedConfigParametersOverrideModule(List.of(subConfigA, otherConfig), false)
        ));
    }
}
