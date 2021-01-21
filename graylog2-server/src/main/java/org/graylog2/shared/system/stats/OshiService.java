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
package org.graylog2.shared.system.stats;


import org.graylog2.shared.utilities.DockerRuntimeDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;

@Singleton
public class OshiService {
    private static final Logger LOG = LoggerFactory.getLogger(OshiService.class);
    private final HardwareAbstractionLayer hal;
    private final OperatingSystem os;


    @Inject
    public OshiService() {
        // Ignore "none" filesystems, which can lead to unwanted errors.
        final ArrayList<String> fsTypes = new ArrayList<>(Arrays.asList(GlobalConfig.get(AbstractFileSystem.OSHI_PSEUDO_FILESYSTEM_TYPES, "").split(",")));
        // Add non-default pseudo filesystem type (Docker related)
        // Avoids warnings like: "WARN : oshi.software.os.linux.LinuxFileSystem - Failed to get information to use statvfs. path: /var/lib/docker/aufs/mnt/422edee4370d8e2553292b2a52b2716967fdf8d344b040c3b821615d5d584961, Error code: 13"
        fsTypes.add("aufs");

        if (DockerRuntimeDetection.isRunningInsideDocker()) {
            // Don't let OSHI filter out "overlay" filesystems when running within Docker.
            // Otherwise we cannot get proper disk statistics
            fsTypes.remove("overlay");
        }
        // Updating the pseudo filesystem types only here only works because the static
        // oshi.software.common.AbstractFileSystem.PSEUDO_FS_TYPES only gets initialized after we update the
        // settings. (because the class hasn't been loaded yet) Should the AbstractFileSystem class at
        // some point get loaded earlier, this setting will have no effect. A solution for this would be to
        // create an upstream PR to make OSHI config usage more dynamic.
        GlobalConfig.set(AbstractFileSystem.OSHI_PSEUDO_FILESYSTEM_TYPES, String.join(",", fsTypes));

        SystemInfo systemInfo = new SystemInfo();
        hal = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();
        LOG.debug("Successfully loaded OSHI");

    }

    public HardwareAbstractionLayer getHal() {
        return hal;
    }

    public OperatingSystem getOs() {
        return os;
    }
}
