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
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog2.Configuration;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.RestTools;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Path("/config.js")
public class AppConfigResource {
    private final Configuration configuration;
    private final Engine templateEngine;

    @Inject
    public AppConfigResource(Configuration configuration, Engine templateEngine) {
        this.configuration = requireNonNull(configuration, "configuration");
        this.templateEngine = requireNonNull(templateEngine, "templateEngine");
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

        final Map<String, Object> model = ImmutableMap.of(
            "rootTimeZone", configuration.getRootTimeZone(),
            "serverUri", RestTools.buildEndpointUri(headers, configuration.getWebEndpointUri()),
            "appPathPrefix", configuration.getWebPrefix());
        return templateEngine.transform(template, model);
    }
}
