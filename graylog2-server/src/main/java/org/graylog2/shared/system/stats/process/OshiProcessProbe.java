package org.graylog2.shared.system.stats.process;

import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
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
