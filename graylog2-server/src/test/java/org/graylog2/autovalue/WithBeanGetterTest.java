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
package org.graylog2.autovalue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class WithBeanGetterTest {
    @Test
    public void testBeanHasJavaBeanGetters() throws NoSuchMethodException {
        final TestBean bean = TestBean.create("Test", true, false);

        final Class<? extends TestBean> beanClass = bean.getClass();
        final Method[] methods = beanClass.getMethods();
        final List<String> methodNames = Arrays.stream(methods)
                .map(Method::getName)
                .collect(Collectors.toList());

        assertThat(methodNames)
                .contains("text", "getText")
                .contains("bool", "isBool")
                .contains("boxedBool", "isBoxedBool");

        assertThat(beanClass.getMethod("text").getAnnotation(JsonProperty.class)).isNotNull();
        assertThat(beanClass.getMethod("getText").getAnnotation(JsonProperty.class)).isNull();
        assertThat(beanClass.getMethod("text").getAnnotation(JsonIgnore.class)).isNull();
        assertThat(beanClass.getMethod("getText").getAnnotation(JsonIgnore.class)).isNotNull();
    }

    interface TestInterface {
        String text();
    }

    @AutoValue
    @WithBeanGetter
    static abstract class TestBean implements TestInterface {
        @JsonProperty
        @Override
        public abstract String text();

        @JsonProperty
        public abstract boolean bool();

        @JsonProperty
        public abstract Boolean boxedBool();

        public static TestBean create(String text, boolean bool, Boolean boxedBool) {
            return new AutoValue_WithBeanGetterTest_TestBean(text, bool, boxedBool);
        }
    }
}
