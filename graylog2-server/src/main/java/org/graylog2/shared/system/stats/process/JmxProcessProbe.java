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

import org.graylog2.shared.SuppressForbidden;

import javax.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

@Singleton
public class JmxProcessProbe implements ProcessProbe {
    private static final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private static final Method openFileDescriptorCountMethod =
            findMethod("getOpenFileDescriptorCount", operatingSystemMXBean.getClass());
    private static final Method maxFileDescriptorCountMethod =
            findMethod("getMaxFileDescriptorCount", operatingSystemMXBean.getClass());
    private static final long pid = findPid();

    @SuppressForbidden("Reflection necessary")
    private static Method findMethod(final String methodName, final Class<?> clazz) {
        try {
            final Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
            return null;
        }
    }

    private static long findPid() {
        try {
            final String processId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return Long.parseLong(processId);
        } catch (Exception e) {
            return -1L;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(final Method method, Object object, T defaultValue) {
        try {
            return (T) method.invoke(object);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    static long getOpenFileDescriptorCount() {
        return invokeMethod(openFileDescriptorCountMethod, operatingSystemMXBean, -1L);
    }

    static long getMaxFileDescriptorCount() {
        return invokeMethod(maxFileDescriptorCountMethod, operatingSystemMXBean, -1L);
    }

    @Override
    public ProcessStats processStats() {
        return ProcessStats.create(pid, getOpenFileDescriptorCount(), getMaxFileDescriptorCount());
    }
}
