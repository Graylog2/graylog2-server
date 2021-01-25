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
package org.graylog2.shared.system.stats.process;

import org.graylog2.shared.system.stats.OshiService;
import oshi.software.os.FileSystem;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import javax.inject.Inject;

public class OshiProcessProbe implements ProcessProbe {

    private final OshiService service;

    @Inject
    public OshiProcessProbe(OshiService service) {
        this.service = service;
    }

    @Override
    public ProcessStats processStats() {
        final OperatingSystem os = service.getOs();

        final FileSystem fs = os.getFileSystem();

        final long pid = os.getProcessId();

        final OSProcess proc = os.getProcess(os.getProcessId());

        final ProcessStats.Cpu cpu = ProcessStats.Cpu.create(((short) proc.getProcessCpuLoadCumulative()), proc.getKernelTime(), proc.getUserTime(), proc.getUpTime());

        final ProcessStats.Memory mem = ProcessStats.Memory.create(proc.getVirtualSize(), proc.getResidentSetSize(), -1);

        return ProcessStats.create(pid, fs.getOpenFileDescriptors(), fs.getMaxFileDescriptors(), cpu, mem);
    }
}
