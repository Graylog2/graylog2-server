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
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.ClassUtils;
import org.graylog.datanode.commands.Server;
import org.graylog.datanode.docs.printers.ConfigFileDocsPrinter;
import org.graylog.datanode.docs.printers.CsvDocsPrinter;
import org.graylog.datanode.docs.printers.DocsPrinter;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ConfigurationDocsGenerator {

    /**
     * This class is linked from the datanode pom.xml and generates conf.example and csv documentation.
     */
    public static void main(String[] args) throws IOException {
        final ConfigurationDocsGenerator generator = new ConfigurationDocsGenerator();
        generator.generateDocumentation(parseDocumentationFormat(args), ConfigurationDocsGenerator::getDatanodeConfigurationBeans);
    }

    @Nonnull
    private static DocumentationFormat parseDocumentationFormat(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("This command needs two arguments - a format and file path. For example" + "csv ${project.build.directory}/configuration-documentation.csv");
        }
        final String format = args[0].toLowerCase(Locale.ROOT).trim();
        final String file = args[1];
        return new DocumentationFormat(format, file);
    }

    protected void generateDocumentation(DocumentationFormat format, Supplier<List<Object>> configurationBeans) throws IOException {
        final List<ConfigurationEntry> configuration = detectConfigurationFields(configurationBeans);
        try (final DocsPrinter writer = createWriter(format)) {
            writer.writeHeader();

            for (ConfigurationEntry f : configuration) {
                writer.writeField(f);
            }
        }
    }

    private DocsPrinter createWriter(DocumentationFormat format) throws IOException {
        final FileWriter fileWriter = new FileWriter(format.outputFile(), StandardCharsets.UTF_8);
        return switch (format.format()) {
            case "csv" -> new CsvDocsPrinter(fileWriter);
            case "conf" -> new ConfigFileDocsPrinter(fileWriter);
            default -> throw new IllegalArgumentException("Unsupported format " + format.format());
        };
    }

    /**
     * Collects all configuration options from all available configuration beans.
     */
    private List<ConfigurationEntry> detectConfigurationFields(Supplier<List<Object>> configurationBeans) {
        return configurationBeans.get()
                .stream()
                .flatMap(ConfigurationDocsGenerator::beanToConfigEntries)
                .sorted(Comparator.comparing(ConfigurationEntry::isPriority).reversed())
                .toList();
    }

    @Nonnull
    private static Stream<ConfigurationEntry> beanToConfigEntries(Object configurationBean) {
        return Arrays.stream(configurationBean.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Parameter.class))
                .filter(ConfigurationDocsGenerator::isPublicFacing)
                .map(f -> toConfigurationEntry(f, configurationBean));
    }

    private static List<Object> getDatanodeConfigurationBeans() {
        return new Server().getCommandConfigurationBeans();
    }

    /**
     * There are some configuration options not intended for general usage, mainly just for system packages configuration.
     *
     * @see Documentation#visible()
     */
    private static boolean isPublicFacing(Field f) {
        return !f.isAnnotationPresent(Documentation.class) || f.getAnnotation(Documentation.class).visible();
    }

    private static ConfigurationEntry toConfigurationEntry(Field f, Object instance) {
        final String documentation = Optional.ofNullable(f.getAnnotation(Documentation.class)).map(Documentation::value).orElse(null);
        final Parameter parameter = f.getAnnotation(Parameter.class);
        final String propertyName = parameter.value();
        final Object defaultValue = getDefaultValue(f, instance);
        final String type = getType(f);
        final boolean required = parameter.required();
        return new ConfigurationEntry(instance.getClass(), f.getName(), type, propertyName, defaultValue, required, documentation);
    }

    private static Object getDefaultValue(Field f, Object instance) {
        try {
            return ReflectionUtils.getFieldValue(instance, f);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getType(Field f) {
        if (f.getType().isPrimitive()) { // unify primitive types and wrappers, e.g. int -> Integer
            return ClassUtils.primitiveToWrapper(f.getType()).getSimpleName();
        } else {
            return f.getType().getSimpleName();
        }
    }
}
