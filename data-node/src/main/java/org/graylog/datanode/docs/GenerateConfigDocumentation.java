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
package org.graylog.datanode.docs;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ReflectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.graylog.datanode.commands.Server;
import org.graylog2.configuration.Documentation;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GenerateConfigDocumentation {

    /**
     * When started, this will output to STDOUT the CSV table of datanode's configuration documentation.
     */
    public static void main(String[] args) throws IOException {
        final List<Object> datanodeConfiguration = new Server().getCommandConfigurationBeans();
        final List<ConfigurationDocumentationPrinter.ConfigurationField> configuration = detectConfigurationFields(datanodeConfiguration);
        try (final ConfigurationDocumentationPrinter writer = createWriter(args)) {
            writer.writeHeader();
            configuration.forEach(f -> {
                try {
                    writer.writeField(f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static ConfigurationDocumentationPrinter createWriter(String[] args) throws IOException {
        assert args.length >= 2;
        final String format = args[0].toLowerCase(Locale.ROOT).trim();
        final String file = args[1];

        final FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8);

        return switch (format) {
            case "csv" -> new CsvConfigurationDocumentationPrinter(fileWriter);
            case "conf" -> new ConfigFilePrinter(fileWriter);
            default -> throw new IllegalArgumentException("Unsupported format " + format);
        };
    }

    private static List<ConfigurationDocumentationPrinter.ConfigurationField> detectConfigurationFields(List<Object> datanodeConfiguration) {
        return datanodeConfiguration.stream()
                .flatMap(configurationBean -> Arrays.stream(configurationBean.getClass().getDeclaredFields()).filter(f -> f.isAnnotationPresent(Parameter.class)).filter(GenerateConfigDocumentation::isPublicFacing).map(f -> toConfigurationField(f, configurationBean)))
                .sorted(Comparator.comparing(ConfigurationDocumentationPrinter.ConfigurationField::isPriority).reversed())
                .toList();
    }

    private static boolean isPublicFacing(Field f) {
        return !f.isAnnotationPresent(Documentation.class) || f.getAnnotation(Documentation.class).visible();
    }

    private static ConfigurationDocumentationPrinter.ConfigurationField toConfigurationField(Field f, Object instance) {

        final String documentation = Optional.ofNullable(f.getAnnotation(Documentation.class)).map(Documentation::value).orElse(null);

        final Parameter parameter = f.getAnnotation(Parameter.class);
        final String propertyName = parameter.value();
        final Object defaultValue = getDefaultValue(f, instance);
        final String type = getType(f);
        final boolean required = parameter.required();
        return new ConfigurationDocumentationPrinter.ConfigurationField(instance.getClass(), f.getName(), type, propertyName, defaultValue, required, documentation);
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
}
