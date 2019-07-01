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
