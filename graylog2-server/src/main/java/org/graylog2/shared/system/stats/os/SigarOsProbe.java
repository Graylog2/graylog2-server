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
package org.graylog2.shared.system.stats.os;

import org.graylog2.shared.system.stats.SigarService;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SigarOsProbe implements OsProbe {
    private final SigarService sigarService;

    @Inject
    public SigarOsProbe(SigarService sigarService) {
        this.sigarService = sigarService;
    }

    @Override
    public OsStats osStats() {
        final Sigar sigar = sigarService.sigar();

        double[] loadAverage;
        try {
            loadAverage = sigar.getLoadAverage();
        } catch (SigarException e) {
            loadAverage = OsStats.EMPTY_LOAD;
        }

        long uptime;
        try {
            uptime = (long) sigar.getUptime().getUptime();
        } catch (SigarException e) {
            uptime = -1L;
        }

        Processor processor;
        try {
            final CpuInfo[] cpuInfos = sigar.getCpuInfoList();

            final String vendor = cpuInfos[0].getVendor();
            final String model = cpuInfos[0].getModel();
            final int mhz = cpuInfos[0].getMhz();
            final int totalCores = cpuInfos[0].getTotalCores();
            final int totalSockets = cpuInfos[0].getTotalSockets();
            final int coresPerSocket = cpuInfos[0].getCoresPerSocket();
            long cacheSize = -1L;
            if (cpuInfos[0].getCacheSize() != Sigar.FIELD_NOTIMPL) {
                cacheSize = cpuInfos[0].getCacheSize();
            }

            final CpuPerc cpuPerc = sigar.getCpuPerc();
            final short sys = (short) (cpuPerc.getSys() * 100);
            final short user = (short) (cpuPerc.getUser() * 100);
            final short idle = (short) (cpuPerc.getIdle() * 100);
            final short stolen = (short) (cpuPerc.getStolen() * 100);

            processor = Processor.create(model, vendor, mhz, totalCores, totalSockets, coresPerSocket, cacheSize,
                    sys, user, idle, stolen);
        } catch (SigarException e) {
            processor = Processor.create("Unknown", "Unknown", -1, -1, -1, -1, -1L,
                    (short) -1, (short) -1, (short) -1, (short) -1);
        }

        Memory memory;
        try {
            Mem mem = sigar.getMem();
            long total = mem.getTotal();
            long free = mem.getFree();
            short freePercent = (short) mem.getFreePercent();
            long used = mem.getUsed();
            short usedPercent = (short) mem.getUsedPercent();
            long actualFree = mem.getActualFree();
            long actualUsed = mem.getActualUsed();

            memory = Memory.create(total, free, freePercent, used, usedPercent, actualFree, actualUsed);
        } catch (SigarException e) {
            memory = Memory.create(-1L, -1L, (short) -1, -1L, (short) -1, -1L, -1L);
        }

        Swap swap;
        try {
            org.hyperic.sigar.Swap sigSwap = sigar.getSwap();
            long total = sigSwap.getTotal();
            long free = sigSwap.getFree();
            long used = sigSwap.getUsed();

            swap = Swap.create(total, free, used);
        } catch (SigarException e) {
            swap = Swap.create(-1L, -1L, -1L);
        }

        return OsStats.create(loadAverage, uptime, processor, memory, swap);
    }
}
