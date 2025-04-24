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
package org.graylog.datanode.docs.printers;

import org.apache.commons.lang.WordUtils;
import org.graylog.datanode.docs.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigFileDocsPrinter implements DocsPrinter {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigFileDocsPrinter.class);

    private final OutputStreamWriter writer;

    public ConfigFileDocsPrinter(OutputStreamWriter writer) {
        this.writer = writer;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void write(List<ConfigurationSection> configurationSections) {
        configurationSections.forEach(section -> doWriteSection(section, 1));
    }

    private void doWriteSection(ConfigurationSection configurationSection, int level) {
        heading(configurationSection.heading(), level).ifPresent(this::append);
        description(configurationSection.description()).ifPresent(this::append);
        configurationSection.entries().stream().map(this::fieldToString).forEach(this::append);
        configurationSection.sections().forEach(section -> doWriteSection(section, level + 1));
    }

    private Optional<String> description(String description) {
        return Optional.ofNullable(description)
                .map(text -> text.lines().map(l -> "# " + l.trim()).collect(Collectors.joining("\n")))
                .map(text -> text + "\n\n");
    }

    private void append(String formatted) {
        try {
            writer.append(formatted);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<String> heading(String heading, int level) {
        return Optional.ofNullable(heading).map(h -> headingTemplate(level).formatted(heading));
    }

    @Nonnull
    private static String headingTemplate(int level) {
        if(level == 1) {
            return """
                    #####################################
                    # %s
                    #####################################
                    """;
        } else {
            return "#### %s\n";
        }
    }

    private String fieldToString(ConfigurationEntry field) {
        // enabled with empty value, force to fill in
        final boolean forceFillIn = field.required() && field.defaultValue() == null;


        String template = """
                %s
                %s%s = %s

                """;

        return String.format(Locale.ROOT, template, formatDocumentation(field), forceFillIn ? "" : "#", field.configName(), wrapValue(field.defaultValue()));
    }

    private String wrapValue(Object value) {
        return Optional.ofNullable(value).map(String::valueOf).orElse("");
    }

    private static String formatDocumentation(ConfigurationEntry field) {
        final String[] lines = Optional.ofNullable(field.documentation()).orElse("").split("\n");
        return Arrays.stream(lines).map(String::trim).peek(line -> {
            if (line.length() > 120) {
                LOG.warn("Documentation line of " + field.configurationBean().getName() + "." + field.fieldName() + " too long, consider splitting into more lines: " + WordUtils.abbreviate(line, 120, 130, "..."));
            }
        }).map(line -> "# " + line).collect(Collectors.joining("\n"));
    }
}
