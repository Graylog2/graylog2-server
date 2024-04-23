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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ReflectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ClassUtils;
import org.bson.assertions.Assertions;
import org.graylog.datanode.commands.Server;
import org.graylog2.configuration.Documentation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class ConfigurationDocumentationTest {

    @Test
    void testAllFieldsAreDocumented() {
        final List<Object> datanodeConfiguration = new Server().getCommandConfigurationBeans();
        final List<Field> undocumentedFields = datanodeConfiguration.stream().flatMap(configurationBean -> {
            return Arrays.stream(configurationBean.getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Parameter.class))
                    .filter(f -> !f.isAnnotationPresent(Documentation.class));
        }).toList();


        if (!undocumentedFields.isEmpty()) {
            final String fields = undocumentedFields.stream()
                    .map(Field::toString)
                    .collect(Collectors.joining("\n"));
            Assertions.fail("Following datanode configuration fields require @Documentation annotation: \n" + fields);
        }
    }

    /**
     * When started, this will output to STDOUT the CSV table of datanode's configuration documentation.
     */
    public static void main(String[] args) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(stringWriter, CSVFormat.EXCEL)) {

            printer.printRecord("Parameter", "Type", "Required", "Default value", "Description");

            final List<Object> datanodeConfiguration = new Server().getCommandConfigurationBeans();

            datanodeConfiguration.forEach(configurationBean -> {
                Arrays.stream(configurationBean.getClass().getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(Parameter.class))
                        .filter(ConfigurationDocumentationTest::isPublicFacing)
                        .map(f -> toConfigurationField(f, configurationBean))
                        .forEach(f -> {
                            try {
                                printer.printRecord(f.configName(), f.type(), f.required(), f.defaultValue(), f.documentation());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            });
            System.out.println(stringWriter);
        }
    }

    private static boolean isPublicFacing(Field f) {
        return !f.isAnnotationPresent(Documentation.class) || f.getAnnotation(Documentation.class).visible();
    }

    private static ConfigurationField toConfigurationField(Field f, Object instance) {

        final String documentation = Optional.ofNullable(f.getAnnotation(Documentation.class))
                .map(Documentation::value).orElse(null);

        final Parameter parameter = f.getAnnotation(Parameter.class);
        final String propertyName = parameter.value();
        final Object defaultValue = getDefaultValue(f, instance);

        final String type = getType(f);

        final boolean required = parameter.required();

        return new ConfigurationField(f.getName(), type, propertyName, defaultValue, required, documentation);
    }

    private static Object getDefaultValue(Field f, Object instance) {
        final Object defaultValue;
        try {
            defaultValue = ReflectionUtils.getFieldValue(instance, f);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return defaultValue;
    }

    private static String getType(Field f) {
        if (f.getType().isPrimitive()) {
            return ClassUtils.primitiveToWrapper(f.getType()).getSimpleName();
        } else {
            return f.getType().getSimpleName();
        }
    }


    private record ConfigurationField(String fieldName, String type, String configName, Object defaultValue,
                                      boolean required, String documentation) {
    }
}
