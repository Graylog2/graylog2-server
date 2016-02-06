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
package org.graylog2.shared.system.stats.process;

import org.graylog2.shared.system.stats.SigarService;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SigarProcessProbe implements ProcessProbe {
    private final SigarService sigarService;

    @Inject
    public SigarProcessProbe(SigarService sigarService) {
        this.sigarService = sigarService;
    }

    @Override
    public synchronized ProcessStats processStats() {
        final Sigar sigar = sigarService.sigar();

        final long pid = sigar.getPid();
        final long openFileDescriptors = JmxProcessProbe.getOpenFileDescriptorCount();
        final long maxFileDescriptorCount = JmxProcessProbe.getMaxFileDescriptorCount();

        ProcessStats.Cpu cpu;
        try {
            ProcCpu procCpu = sigar.getProcCpu(pid);
            cpu = ProcessStats.Cpu.create(
                    (short) (procCpu.getPercent() * 100),
                    procCpu.getSys(),
                    procCpu.getUser(),
                    procCpu.getTotal());
        } catch (SigarException e) {
            cpu = null;
        }

        ProcessStats.Memory memory;
        try {
            ProcMem mem = sigar.getProcMem(sigar.getPid());
            memory = ProcessStats.Memory.create(
                    mem.getSize(),
                    mem.getResident(),
                    mem.getShare());
        } catch (SigarException e) {
            memory = null;
        }

        return ProcessStats.create(pid, openFileDescriptors, maxFileDescriptorCount, cpu, memory);
    }
}
