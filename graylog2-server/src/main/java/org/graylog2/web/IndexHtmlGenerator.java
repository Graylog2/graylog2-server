package org.graylog2.web;

import com.floreysoft.jmte.Engine;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IndexHtmlGenerator {
    private final Engine engine = new Engine();
    private final String content;

    @Inject
    public IndexHtmlGenerator(final String title, final Collection<String> cssFiles, final Collection<String> jsFiles) throws IOException {
        final String template = IOUtils.toString(ClassLoader.getSystemResourceAsStream("web-interface/index.html.template"));
        final Map<String, Object> model = new HashMap<String, Object>() {{
            put("title", title);
            put("cssFiles", cssFiles);
            put("jsFiles", jsFiles);
        }};

        this.content = engine.transform(template, model);
    }

    public String get() {
        return this.content;
    }
}
