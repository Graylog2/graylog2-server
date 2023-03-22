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
package org.graylog.datanode.bootstrap;

import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpensearchBinDirExistsCheck implements PreflightCheck {

    private final String opensearchLocation;

    @Inject
    public OpensearchBinDirExistsCheck(@Named("opensearch_location") String opensearchLocation) {
        this.opensearchLocation = opensearchLocation;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        if (!Files.isDirectory(Path.of(opensearchLocation))) {
            throw new PreflightCheckException("Opensearch base directory " + opensearchLocation + " doesn't exist!");
        }
    }
}
