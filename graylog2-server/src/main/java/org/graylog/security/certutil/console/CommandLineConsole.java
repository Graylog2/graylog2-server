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
package org.graylog.security.certutil.console;

import java.util.Locale;

public interface CommandLineConsole {
    String readLine(Prompt prompt) throws ConsoleException;
    char[] readPassword(Prompt prompt) throws ConsoleException;

    default boolean readBoolean(Prompt prompt) {
        final String response = readLine(prompt)
                .trim().toLowerCase(Locale.ROOT);
        return response.equals("y") || response.equals("yes");
    }

    default int readInt(Prompt prompt) {
        final String response = readLine(prompt)
                .trim().toLowerCase(Locale.ROOT);
        return Integer.parseInt(response);
    }

    void printLine(String line);

    static Prompt prompt(String query) {
        return new Prompt(query);
    }

    record Prompt(String question) {
    }
}
