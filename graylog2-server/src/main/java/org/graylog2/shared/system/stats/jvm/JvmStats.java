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
package org.graylog2.shared.system.stats.jvm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class JvmStats {
    public static final JvmStats INSTANCE;

    static {
        final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        final long heapInit = memoryMXBean.getHeapMemoryUsage().getInit();
        final long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        final long nonHeapInit = memoryMXBean.getNonHeapMemoryUsage().getInit();
        final long nonHeapMax = memoryMXBean.getNonHeapMemoryUsage().getMax();
        long directMemoryMax;
        try {
            Class<?> vmClass = Class.forName("sun.misc.VM");
            directMemoryMax = (long) vmClass.getMethod("maxDirectMemory").invoke(null);
        } catch (Throwable t) {
            directMemoryMax = -1;
        }
        final Memory memory = Memory.create(heapInit, heapMax, nonHeapInit, nonHeapMax, directMemoryMax);

        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        final long startTime = runtimeMXBean.getStartTime();
        final String version = runtimeMXBean.getSystemProperties().get("java.version");
        final String vmName = runtimeMXBean.getVmName();
        final String vmVendor = runtimeMXBean.getVmVendor();
        final String vmVersion = runtimeMXBean.getVmVersion();

        final String specName = runtimeMXBean.getSpecName();
        final String specVendor = runtimeMXBean.getSpecVendor();
        final String specVersion = runtimeMXBean.getSpecVersion();

        final List<String> inputArguments = runtimeMXBean.getInputArguments();
        String bootClassPath;
        try {
            bootClassPath = runtimeMXBean.getBootClassPath();
        } catch (UnsupportedOperationException e) {
            bootClassPath = "<unsupported>";
        }
        final String classPath = runtimeMXBean.getClassPath();

        // TODO Remove some sensitive values or don't output at all?
        final Map<String, String> systemProperties = runtimeMXBean.getSystemProperties();

        final List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        final List<String> garbageCollectors = new ArrayList<>(gcMxBeans.size());
        for (GarbageCollectorMXBean gcMxBean : gcMxBeans) {
            garbageCollectors.add(gcMxBean.getName());
        }

        final List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        final List<String> memoryPools = new ArrayList<>(memoryPoolMXBeans.size());
        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            memoryPools.add(memoryPoolMXBean.getName());
        }

        INSTANCE = JvmStats.create(version, vmName, vmVersion, vmVendor, specName, specVersion, specVendor, startTime, memory, inputArguments, bootClassPath, classPath, systemProperties, garbageCollectors, memoryPools);
    }

    @JsonProperty
    public abstract String version();

    @JsonProperty
    public abstract String vmName();

    @JsonProperty
    public abstract String vmVersion();

    @JsonProperty
    public abstract String vmVendor();

    @JsonProperty
    public abstract String specName();

    @JsonProperty
    public abstract String specVersion();

    @JsonProperty
    public abstract String specVendor();

    @JsonProperty
    public abstract long startTime();

    @JsonProperty
    public abstract Memory mem();

    @JsonProperty
    public abstract List<String> inputArguments();

    @JsonProperty
    public abstract String bootClassPath();

    @JsonProperty
    public abstract String classPath();

    @JsonProperty
    public abstract Map<String, String> systemProperties();

    @JsonProperty
    public abstract List<String> garbageCollectors();

    @JsonProperty
    public abstract List<String> memoryPools();


    public static JvmStats create(String version,
                                  String vmName,
                                  String vmVersion,
                                  String vmVendor,
                                  String specName,
                                  String specVersion,
                                  String specVendor,
                                  long startTime,
                                  JvmStats.Memory mem,
                                  List<String> inputArguments,
                                  String bootClassPath,
                                  String classPath,
                                  Map<String, String> systemProperties,
                                  List<String> garbageCollectors,
                                  List<String> memoryPools) {
        return new AutoValue_JvmStats(
                version, vmName, vmVersion, vmVendor, specName, specVersion, specVendor,
                startTime, mem, inputArguments, bootClassPath, classPath, systemProperties,
                garbageCollectors, memoryPools);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Memory {
        @JsonProperty
        public abstract long heapInit();

        @JsonProperty
        public abstract long heapMax();

        @JsonProperty
        public abstract long nonHeapInit();

        @JsonProperty
        public abstract long nonHeapMax();

        @JsonProperty
        public abstract long directMemoryMax();

        public static Memory create(long heapInit,
                                    long heapMax,
                                    long nonHeapInit,
                                    long nonHeapMax,
                                    long directMemoryMax) {
            return new AutoValue_JvmStats_Memory(heapInit, heapMax, nonHeapInit, nonHeapMax, directMemoryMax);
        }
    }
}
