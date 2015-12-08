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

import org.graylog2.plugin.Plugin;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

@Path("/plugin/{plugin}/{filename}")
public class PluginAssetsResource {
    private final Set<Plugin> plugins;

    @Inject
    public PluginAssetsResource(Set<Plugin> plugins) {
        this.plugins = plugins;
    }

    @GET
    public Response get(@PathParam("plugin") String pluginName,
                        @PathParam("filename") String filename) {
        final Optional<Plugin> plugin = getPluginForName(pluginName);
        if (!plugin.isPresent()) {
            throw new NotFoundException();
        }

        final InputStream stream = getStreamForPluginFile(plugin.get(), filename);
        if (stream == null) {
            throw new NotFoundException();
        }

        final String contentType = firstNonNull(URLConnection.guessContentTypeFromName(filename), MediaType.APPLICATION_OCTET_STREAM);
        return Response
                .ok(stream)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .build();
    }

    private InputStream getStreamForPluginFile(Plugin plugin, String filename) {
        return plugin.metadata().getClass().getResourceAsStream("/" + filename);
    }

    private Optional<Plugin> getPluginForName(String pluginName) {
        return this.plugins.stream().filter(plugin -> plugin.metadata().getUniqueId().equals(pluginName)).findFirst();
    }
}
