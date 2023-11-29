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
package org.graylog.datanode.bootstrap.preflight;

import org.apache.commons.exec.OS;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Check required system parameters
 */
public class OpenSearchPreconditionsCheck implements PreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchPreconditionsCheck.class);
    private static final long MAX_MAP_COUNT_MIN = 262144L;

    @Override
    public void runCheck() throws PreflightCheckException {
        if (OS.isFamilyMac()) {
            return;
        }

        ProcessBuilder builder = new ProcessBuilder().redirectErrorStream(true);
        builder.command("/sbin/sysctl", "-n", "vm.max_map_count");
        try {
            final Process process = builder.start();
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
            String line = reader.readLine();
            reader.close();
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                System.out.println("Sysctl failed with error return " + exitVal);
                return;
            }
            if (line != null) {
                long count = Long.valueOf(line);
                if (count < MAX_MAP_COUNT_MIN) {
                    throw new RuntimeException("vm.max_map_count = " + count + " but should be at least " + MAX_MAP_COUNT_MIN);
                }
            }
        } catch (IOException | InterruptedException e) {
            LOG.warn("Failed to run sysctl check: {}", e.getMessage(), e);
        }
    }
}
