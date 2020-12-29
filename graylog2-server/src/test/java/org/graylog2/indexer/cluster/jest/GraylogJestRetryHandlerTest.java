/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster.jest;

import io.searchbox.client.JestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GraylogJestRetryHandlerTest {
    private JestRetryHandler<HttpUriRequest> retryHandler;

    @BeforeEach
    void setUp() {
        this.retryHandler = new GraylogJestRetryHandler(3);
    }

    @Test
    void retriesSocketTimeouts() {
        assertThat(this.retryHandler.retryRequest(new SocketTimeoutException(), 0, new HttpGet("http://localhost"))).isTrue();
    }
}
