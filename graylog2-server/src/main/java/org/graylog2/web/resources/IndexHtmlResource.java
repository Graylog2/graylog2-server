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
