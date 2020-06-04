/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.system.stats;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OshiService {
    private static final Logger LOG = LoggerFactory.getLogger(OshiService.class);
    private final HardwareAbstractionLayer hal;
    private final OperatingSystem os;


    @Inject
    public OshiService() {
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
