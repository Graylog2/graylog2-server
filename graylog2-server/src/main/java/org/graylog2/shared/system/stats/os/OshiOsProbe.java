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

import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.util.Util;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class OshiOsProbe implements OsProbe {

    private final OshiService service;

    @Inject
    public OshiOsProbe(OshiService service) {
        this.service = service;
    }

    @Override
    public OsStats osStats() {

        final HardwareAbstractionLayer hardware = service.getHal();

        final GlobalMemory globalMemory = hardware.getMemory();

        final Memory mem = Memory.create(
                globalMemory.getTotal(),
                globalMemory.getAvailable(),
                safePercentage(globalMemory.getAvailable(), globalMemory.getTotal(), 0),
                globalMemory.getTotal() - globalMemory.getAvailable(),
                safePercentage(globalMemory.getTotal() - globalMemory.getAvailable(), globalMemory.getTotal(), 0),
                globalMemory.getAvailable(),
                globalMemory.getTotal() - globalMemory.getAvailable());

        final VirtualMemory virtualMemory = globalMemory.getVirtualMemory();

        final Swap swap = Swap.create(virtualMemory.getSwapTotal(), virtualMemory.getSwapTotal() - virtualMemory.getSwapUsed(), virtualMemory.getSwapUsed());

        final CentralProcessor centralProcessor = hardware.getProcessor();

        long[] prevTicks = centralProcessor.getSystemCpuLoadTicks();
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = centralProcessor.getSystemCpuLoadTicks();

        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long system = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];

        long totalCpu = user + nice + system + idle + iowait + irq + softirq + steal;
        totalCpu = totalCpu == 0 ? 1 : totalCpu; // avoid division by zero

        short sys = (short) (100 * system / totalCpu);
        short us = (short) (100 * user / totalCpu);
        short id = (short) (100 * idle / totalCpu);
        short st = (short) (100 * steal / totalCpu);

        final CentralProcessor.ProcessorIdentifier processorIdentifier = centralProcessor.getProcessorIdentifier();
        final int totalSockets = centralProcessor.getPhysicalPackageCount() > 0 ? centralProcessor.getPhysicalPackageCount() : 1;

        final Processor proc = Processor.create(
                processorIdentifier.getName(),
                processorIdentifier.getVendor(),
                (int) (processorIdentifier.getVendorFreq() / 1000000),
                centralProcessor.getLogicalProcessorCount(),
                centralProcessor.getPhysicalPackageCount(),
                centralProcessor.getLogicalProcessorCount() / totalSockets,
                -1,
                sys,
                us,
                id,
                st);


        return OsStats.create(
                centralProcessor.getSystemLoadAverage(3),
                service.getOs().getSystemUptime(),
                proc,
                mem,
                swap);
    }

    private short safePercentage(long nominator, long denominator, int override) {
        return (denominator == 0) ? (short) override : (short) (nominator * 100 / denominator);
    }
}
