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
package org.graylog2;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import io.netty.handler.ssl.OpenSsl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyTest {
    @Test
    @EnabledOnOs(value = OS.LINUX)
    public void epollIsAvailableOnLinux() {
        assertTrue(Epoll.isAvailable());
    }

    @Test
    @EnabledOnOs(value = OS.MAC)
    public void kqueueIsAvailableOnMac() {
        assertTrue(KQueue.isAvailable());
    }

    @Test
    public void boringSslIsAvailable() {
        assertTrue(OpenSsl.isAvailable());
        assertEquals("BoringSSL", OpenSsl.versionString());
    }
}
