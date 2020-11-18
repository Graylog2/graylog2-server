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
package org.graylog2.rest;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringEscapeUtils;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.Request;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;

public class GraylogErrorPageGenerator implements ErrorPageGenerator {
    private final String template;
    private final Engine engine;

    @Inject
    public GraylogErrorPageGenerator(Engine templateEngine) throws IOException {
        this(Resources.toString(Resources.getResource("error.html.template"), StandardCharsets.UTF_8), templateEngine);
    }

    private GraylogErrorPageGenerator(String template, Engine templateEngine) {
        this.template = requireNonNull(template, "template");
        this.engine = requireNonNull(templateEngine, "templateEngine");
    }

    @Override
    public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception) {
        final ImmutableMap.Builder<String, Object> modelBuilder = ImmutableMap.builder();
        modelBuilder.put("reason", StringEscapeUtils.escapeHtml4(reasonPhrase));

        if (description != null) {
            modelBuilder.put("description", StringEscapeUtils.escapeHtml4(description));
        }

        if (exception != null) {
            String stacktrace = "";
            try (final StringWriter stringWriter = new StringWriter();
                 final PrintWriter printWriter = new PrintWriter(stringWriter)) {
                exception.printStackTrace(printWriter);
                printWriter.flush();
                stringWriter.flush();
                stacktrace = stringWriter.toString();
            } catch (IOException ignored) {
                // Ignore
            }
            modelBuilder
                    .put("exception", StringEscapeUtils.escapeHtml4(exception.getMessage()))
                    .put("stacktrace", StringEscapeUtils.escapeHtml4(stacktrace));
        }

        return engine.transform(template, modelBuilder.build());
    }
}
