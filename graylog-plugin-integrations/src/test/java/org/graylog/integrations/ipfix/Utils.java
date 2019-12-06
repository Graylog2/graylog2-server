package org.graylog.integrations.ipfix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;

public class Utils {
    public static ByteBuf readPacket(String resourceName) throws IOException {
        return Unpooled.wrappedBuffer(toByteArray(getResource(resourceName)));
    }
}
