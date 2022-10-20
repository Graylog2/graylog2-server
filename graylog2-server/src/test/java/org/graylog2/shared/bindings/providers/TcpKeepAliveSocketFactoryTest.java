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
