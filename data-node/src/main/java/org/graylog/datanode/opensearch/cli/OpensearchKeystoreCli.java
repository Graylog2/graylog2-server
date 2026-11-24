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
package org.graylog.datanode.opensearch.cli;

import org.graylog.datanode.process.Environment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OpensearchKeystoreCli extends AbstractOpensearchCli {

    public static final String KEYSTORE_BIN_NAME = "opensearch-keystore";

    public OpensearchKeystoreCli(Path binDir, Environment env) {
        super(binDir.resolve(KEYSTORE_BIN_NAME), env);
    }

    /**
     * Create a new opensearch keystore. This command expects that there is no keystore. If there is a keystore,
     * it will respond YES to override existing.
     *
     * @return STDOUT/STDERR of the execution as one String
     */
    public String create() {
        return runWithStdin(Collections.singletonList("Y"), "create");
    }

    /**
     * Add secrets to the store. The command is interactive, it will ask for the secret value (to avoid recording the value
     * in the command line history). So we have to work around that and provide the value in STDIN.
     */
    public void add(String key, String secretValue) {
        runWithStdin(List.of(secretValue), "add", "-x", key); // -x allows input from stdin, bypassing the prompt
    }

    /**
     * Add several secrets to the store with one command. Every command starts a JVM, which takes seconds, so this is
     * much faster than adding the secrets one by one. The values are read from STDIN, one line each, like
     * {@link #add} reads its single value.
     */
    public void addAll(Map<String, String> secrets) {
        if (secrets.isEmpty()) {
            return;
        }
        final List<String> keys = List.copyOf(secrets.keySet());
        final List<String> args = new ArrayList<>(List.of("add", "-x"));
        args.addAll(keys);
        runWithStdin(keys.stream().map(secrets::get).toList(), args.toArray(String[]::new));
    }

    /**
     * Add secrets to the store. The command is interactive, it will ask for the secret value (to avoid recording the value
     * in the command line history). So we have to work around that and provide the value in STDIN.
     */
    public void addFile(String key, Path file) {
        runWithStdin(List.of(), "add-file", key, file.toAbsolutePath().toString()); // -x allows input from stdin, bypassing the prompt
    }

    /**
     * Add a file secret to the store without writing its content to disk. The add-file command accepts only a path,
     * so it reads the content from its own STDIN and the content is piped there.
     */
    public void addFile(String key, byte[] content) {
        runWithStdin(content, "add-file", key, "/dev/stdin");
    }

    public List<String> list() {
        final String rawResponse = runWithStdin(Collections.emptyList(), "list");
        final String[] items = rawResponse.split("\n");
        return Arrays.asList(items);
    }
}
