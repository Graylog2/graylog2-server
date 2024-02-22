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

import java.nio.file.Path;
import java.util.Collections;

public class OpensearchKeystoreCli extends AbstractOpensearchCli {
    public OpensearchKeystoreCli(Path configPath, Path binPath) {
        super(configPath, binPath.resolve("opensearch-keystore"));
    }

    public String create() {
        return run("create");
    }

    public void add(String key, String secretValue) {
        run(Collections.singletonList(secretValue), "add", key);
    }
}
