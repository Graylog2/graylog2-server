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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * This is a wrapper around System.console, which is not available inside IDE. Additionally, the CommandLineConsole
 * interface makes the testing easier, with implementation that can expect prompts and deliver preconfigured values
 */
public class SystemConsole implements CommandLineConsole {

    public String readLine(String format, Object... args) {
        if (System.console() != null) {
            return System.console().readLine(format, args);
        } else {
            printLine(String.format(Locale.ROOT, format, args));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            try {
                return reader.readLine();
            } catch (IOException e) {
                throw new ConsoleException(e);
            }
        }
    }

    public char[] readPassword(String format, Object... args) {
        if (System.console() != null) {
            return System.console().readPassword(format, args);
        } else {
            return readLine(format, args).toCharArray();
        }
    }

    @Override
    public void printLine(String line) {
        System.out.println(line);
    }
}
