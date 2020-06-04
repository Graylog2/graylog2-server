package org.graylog2.shared.system.stats.os;

import org.graylog2.shared.system.stats.OshiService;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;

import javax.inject.Inject;
import javax.inject.Singleton;

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
            (short) (globalMemory.getAvailable() * 100 / globalMemory.getTotal()),
            globalMemory.getTotal() - globalMemory.getAvailable(),
            (short) ((globalMemory.getTotal() - globalMemory.getAvailable()) * 100 / globalMemory.getTotal()),
            globalMemory.getAvailable(),
            globalMemory.getTotal() - globalMemory.getAvailable());

        final VirtualMemory virtualMemory = globalMemory.getVirtualMemory();

        final Swap swap = Swap.create(virtualMemory.getSwapTotal(),  virtualMemory.getSwapTotal() - virtualMemory.getSwapUsed(), virtualMemory.getSwapUsed());

        final CentralProcessor centralProcessor = hardware.getProcessor();

        long[] prevTicks = centralProcessor.getSystemCpuLoadTicks();
        long[] ticks = centralProcessor.getSystemCpuLoadTicks();
        short user = (short) (ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()]);
        short sys = (short) (ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()]);
        short idle = (short) (ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()]);
        short steal = (short) (ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()]);

        final CentralProcessor.ProcessorIdentifier processorIdentifier = centralProcessor.getProcessorIdentifier();

        final Processor proc = Processor.create(
                processorIdentifier.getModel(),
                processorIdentifier.getVendor(),
                ((int) processorIdentifier.getVendorFreq()/1000),
                centralProcessor.getLogicalProcessorCount(),
                centralProcessor.getPhysicalPackageCount(),
                centralProcessor.getLogicalProcessorCount() / centralProcessor.getPhysicalPackageCount(),
                -1,
                sys,
                user,
                idle,
                steal);


        return OsStats.create(
                centralProcessor.getSystemLoadAverage(3),
                service.getOs().getSystemUptime(),
                proc,
                mem,
                swap);
    }
}
