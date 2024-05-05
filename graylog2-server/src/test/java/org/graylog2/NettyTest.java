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
