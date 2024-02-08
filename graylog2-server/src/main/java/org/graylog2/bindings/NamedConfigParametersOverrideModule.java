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
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.joschi.jadconfig.ReflectionUtils.getAllFields;
import static com.github.joschi.jadconfig.ReflectionUtils.getFieldValue;
import static com.google.inject.name.Names.named;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * A Guice module which registers all fields of the provided objects annotated with {@link Parameter}
 * as named bindings and optionally also adds bindings of the object instances themselves.
 * For more flexibility, getter methods can also be used, which must be annotated with {@link NamedBindingOverride}.
 * This class is based off {@link com.github.joschi.jadconfig.guice.NamedConfigParametersModule}
 *
 * @see com.google.inject.name.Named
 */
public class NamedConfigParametersOverrideModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(NamedConfigParametersOverrideModule.class);

    private final Set<Object> beans;
    private final boolean registerBeans;


    public NamedConfigParametersOverrideModule(final Collection<?> beans, final boolean registerBeans) {
        this.beans = new HashSet<>(beans);
        this.registerBeans = registerBeans;
    }

    public NamedConfigParametersOverrideModule(final Collection<?> beans) {
        this(beans, true);
    }

    @Override
    protected void configure() {
        if (registerBeans) {
            registerBeanInstances();
        }

        for (final Object bean : beans) {
            registerParameters(bean);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerParameters(Object bean) {
        final Field[] fields = getAllFields(bean.getClass());
        Map<String, Method> methodOverrides = ReflectionUtils.getAllMethods(bean.getClass()).stream()
                .filter(obj -> Objects.nonNull(obj.getAnnotation(NamedBindingOverride.class)))
                .collect(Collectors.toMap(obj -> obj.getAnnotation(NamedBindingOverride.class).value(), Function.identity()));

        for (Field field : fields) {
            final Parameter parameter = field.getAnnotation(Parameter.class);

            if (parameter != null) {
                try {
                    final TypeLiteral typeLiteral = TypeLiteral.get(field.getGenericType());
                    Object value = getFieldValue(bean, field);
                    String parameterName = parameter.value();
                    if (methodOverrides.containsKey(parameterName)) {
                        Method method = methodOverrides.get(parameterName);
                        TypeLiteral<?> methodTypeLiteral = TypeLiteral.get(method.getGenericReturnType());
                        if (!typeLiteral.equals(methodTypeLiteral)) {
                            throw new IllegalStateException(f("Parameter %s type mismatch @%s#%s <> @%s#%s",
                                    parameterName,
                                    NamedBindingOverride.class.getSimpleName(),
                                    methodTypeLiteral,
                                    Parameter.class.getSimpleName(),
                                    typeLiteral
                            ));
                        }
                        value = ReflectionUtils.invoke(method, bean);
                    }

                    if (value == null) {
                        bind(typeLiteral).annotatedWith(named(parameterName)).toProvider(Providers.of(null));
                    } else {
                        bind(typeLiteral).annotatedWith(named(parameterName)).toInstance(value);
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Couldn't bind \"" + field.getName() + "\"", e);
                }
            } else {
                LOG.debug("Skipping field {}", field.getName());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerBeanInstances() {
        for (final Object bean : beans) {
            final TypeLiteral typeLiteral = TypeLiteral.get(bean.getClass());
            bind(typeLiteral).toInstance(bean);
        }
    }
}
