package org.graylog2.web.resources;

import com.floreysoft.jmte.Engine;
import org.apache.commons.io.IOUtils;
import org.graylog2.Configuration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/config.js")
public class AppConfigResource {
    private static final Engine engine = new Engine();
    private final String content;

    @Inject
    public AppConfigResource(Configuration configuration) throws IOException {
        final String template = IOUtils.toString(ClassLoader.getSystemResourceAsStream("web-interface/config.js.template"));
        final Map<String, Object> model = new HashMap<String, Object>() {{
            put("serverUri", configuration.getRestTransportUri());
            put("appPathPrefix", "");
        }};
        this.content = engine.transform(template, model);
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public String get() {
        return this.content;
    }
}
