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
package org.graylog2.shared.bindings.providers;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.Socket;

class TcpKeepAliveSocketFactoryTest {

    @Test
    void testNullDelegate() {
        Assertions.assertThatThrownBy(() -> new TcpKeepAliveSocketFactory(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void keepAliveFlagIsSet() throws IOException {
        final TcpKeepAliveSocketFactory factory = new TcpKeepAliveSocketFactory(SocketFactory.getDefault());
        final Socket socket = factory.createSocket();
        Assertions.assertThat(socket.getKeepAlive()).isTrue();
        socket.close();
    }
}
