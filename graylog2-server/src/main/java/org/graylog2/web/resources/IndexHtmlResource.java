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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.emory.mathcs.backport.java.util.Collections;
import org.graylog2.plugin.Plugin;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.PackageManifest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Path("/index.html")
public class IndexHtmlResource {
    private final IndexHtmlGenerator indexHtmlGenerator;

    @Inject
    public IndexHtmlResource(Set<Plugin> plugins, ObjectMapper objectMapper) throws IOException {
        final InputStream packageManifest = ClassLoader.getSystemResourceAsStream("web-interface/assets/module.json");
        final PackageManifest manifest = objectMapper.readValue(packageManifest, PackageManifest.class);
        final List<String> jsFiles = (List<String>)manifest.files().get("js");
        this.indexHtmlGenerator = new IndexHtmlGenerator("Graylog Web Interface", Collections.emptySet(), jsFiles);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String get() {
        return this.indexHtmlGenerator.get();
    }
}
