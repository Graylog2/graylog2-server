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
package org.graylog.datanode.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class StreamConsumer implements Runnable {
    private final InputStream inputStream;
    private final Consumer<String> consumeInputLine;

    public StreamConsumer(InputStream inputStream, Consumer<String> consumeInputLine) {
        this.inputStream = inputStream;
        this.consumeInputLine = consumeInputLine;
    }

    public void run() {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            // the readLine blocks till the underlying stream either produces some data or terminates
            // there is no clean way how to interrupt this waiting thread
            while ((line = br.readLine()) != null) {
                consumeInputLine.accept(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
