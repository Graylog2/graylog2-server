/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package selenium.serverstub;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import com.google.common.collect.Maps;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import javax.ws.rs.core.UriBuilder;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ServerStub {

    public Map<String, String> users = Maps.newHashMap();

    private final int restPort;

    private HttpServer httpServer;

    public ServerStub(int restPort) {
        this.restPort = restPort;
    }

    public void initialize() {
        try {
            startRestServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void kill() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    private HttpServer startRestServer() throws IOException {
        URI restUri = UriBuilder.fromUri("http://0.0.0.0/").port(restPort).build();
        ResourceConfig rc = new PackagesResourceConfig("selenium.serverstub.rest.resources");
        rc.getProperties().put("core", this);

        System.out.println("Started stub REST API at " + restUri);

        return GrizzlyServerFactory.createHttpServer(restUri, rc);
    }

}
