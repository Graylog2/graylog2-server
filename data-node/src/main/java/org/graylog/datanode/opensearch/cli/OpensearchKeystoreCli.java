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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OpensearchKeystoreCli extends AbstractOpensearchCli {

    public OpensearchKeystoreCli(Path configDir, Path binDir) {
        super(configDir, binDir, "opensearch-keystore");
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

    public List<String> list() {
        final String rawResponse = runWithStdin(Collections.emptyList(), "list");
        final String[] items = rawResponse.split("\n");
        return Arrays.asList(items);
    }
}
