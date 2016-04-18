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
package org.graylog2.web.resources;

import com.floreysoft.jmte.Engine;
import com.google.common.io.Resources;
import org.graylog2.Configuration;
import org.graylog2.rest.MoreMediaTypes;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/config.js")
public class AppConfigResource {
    private static final String CK_OVERRIDE_SERVER_URI = "X-Graylog-Server-URL";
    private static final Engine engine = new Engine();
    private final Configuration configuration;

    @Inject
    public AppConfigResource(Configuration configuration) {
        this.configuration = configuration;
    }

    @GET
    @Produces(MoreMediaTypes.APPLICATION_JAVASCRIPT)
    public String get(@Context HttpHeaders headers) {
        final URL templateUrl = this.getClass().getResource("/web-interface/config.js.template");
        final String template;
        try {
            template = Resources.toString(templateUrl, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read AppConfig template while generating web interface configuration: ", e);
        }

        final Map<String, Object> model = new HashMap<String, Object>() {{
            put("rootTimeZone", configuration.getRootTimeZone());
            put("serverUri", buildEndpointUri(headers));
            put("appPathPrefix", "");
        }};
        return engine.transform(template, model);
    }

    private String buildEndpointUri(HttpHeaders httpHeaders) {
        Optional<String> endpointUri = Optional.empty();
        final List<String> headers = httpHeaders.getRequestHeader(CK_OVERRIDE_SERVER_URI);
        if (headers != null && !headers.isEmpty()) {
            endpointUri = headers.stream().filter(s -> {
                try {
                    final URL url = new URL(s);
                    switch (url.getProtocol()) {
                        case "http":
                        case "https":
                            return true;
                        default:
                            return false;
                    }
                } catch (MalformedURLException e) {
                    return false;
                }
            }).findFirst();
        }

        return endpointUri.orElse(configuration.getWebEndpointUri().toString());
    }
}
