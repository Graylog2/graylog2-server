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
package org.graylog.datanode.management.opensearch.cli;

import org.graylog.datanode.state.OpensearchConfiguration;

import java.util.Collections;

public class OpensearchKeystoreCli extends AbstractOpensearchCli {

    OpensearchKeystoreCli(OpensearchConfiguration config) {
        super(config, "opensearch-keystore");
    }

    /**
     * Create a new opensearch keystore. This command expects that there is no keystore. If there is a keystore,
     * it will respond YES to override existing.
     * @return STDOUT/STDERR of the execution as one String
     */
    public String create() {
        return runWithStdin(Collections.singletonList("Y"),"create");
    }

    /**
     * Add secrets to the store. The command is interactive, it will ask for the secret value (to avoid recording the value
     * in the command line history). So we have to work around that and provide the value in STDIN.
     */
    public void add(String key, String secretValue) {
        runWithStdin(Collections.singletonList(secretValue), "add", key);
    }
}
