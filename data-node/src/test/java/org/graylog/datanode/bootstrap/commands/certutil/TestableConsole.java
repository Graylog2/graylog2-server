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
package org.graylog.datanode.bootstrap.commands.certutil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TestableConsole implements CommandLineConsole {

    private static final Logger LOG = LoggerFactory.getLogger(TestableConsole.class);

    private final List<Pair<String, String>> providedResponses = new LinkedList<>();
    private final List<String> output = new LinkedList<>();
    private boolean silent = false;

    public static TestableConsole empty() {
        return new TestableConsole();
    }

    public TestableConsole silent() {
        this.silent = true;
        return this;
    }

    public TestableConsole register(String question, String response) {
        this.providedResponses.add(Pair.of(cleanQuestion(question), response));
        return this;
    }

    @Override
    public String readLine(String format, Object... args) {
        final Pair<String, String> response = consumeResponse(format, args);
        return response.getValue();
    }

    @Override
    public char[] readPassword(String format, Object... args) {
        final Pair<String, String> response = consumeResponse(format, args);
        return response.getValue().toCharArray();
    }

    @Override
    public void printLine(String line) {
        if (!silent) {
            LOG.info(line);
        }
        this.output.add(line);
    }

    public List<String> getOutput() {
        return output;
    }

    private Pair<String, String> consumeResponse(String format, Object[] args) {
        final String question = cleanQuestion(String.format(Locale.ROOT, format, args));
        final Pair<String, String> response = providedResponses.stream().filter(r -> r.getKey().equals(question)).findFirst()
                .orElseThrow(() -> new ConsoleException("Unexpected input question:" + question));
        providedResponses.remove(response);
        return response;
    }

    private String cleanQuestion(String input) {
        return Optional.of(input)
                .map(String::trim)
                .map(s -> StringUtils.removeEnd(s, ":"))
                .map(String::trim)
                .get();
    }
}
