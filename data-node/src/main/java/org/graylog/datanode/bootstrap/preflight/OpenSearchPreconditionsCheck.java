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

import com.sun.jna.Platform;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.PlatformEnum;
import oshi.util.FileUtil;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Check required system parameters.
 */
public class OpenSearchPreconditionsCheck implements PreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchPreconditionsCheck.class);

    // See: https://opensearch.org/docs/2.11/install-and-configure/install-opensearch/index/#important-settings
    private static final long MAX_MAP_COUNT_MIN = 262144L;
    private static final String PROC_SYS_VM_MAX_MAP_COUNT_PATH = "/proc/sys/vm/max_map_count";

    @Override
    public void runCheck() throws PreflightCheckException {
        if (PlatformEnum.getValue(Platform.getOSType()) != PlatformEnum.LINUX) {
            LOG.debug("Check only supports Linux operating systems");
            return;
        }

        final var vmMaxMapCount = FileUtil.getLongFromFile(PROC_SYS_VM_MAX_MAP_COUNT_PATH);

        if (vmMaxMapCount == 0) {
            LOG.warn("Couldn't read value from {}", PROC_SYS_VM_MAX_MAP_COUNT_PATH);
        } else if (vmMaxMapCount < MAX_MAP_COUNT_MIN) {
            throw new PreflightCheckException(f("%s value should be at least %d but is %d (set via \"vm.max_map_count\" sysctl)",
                    PROC_SYS_VM_MAX_MAP_COUNT_PATH, MAX_MAP_COUNT_MIN, vmMaxMapCount));
        }
    }
}
