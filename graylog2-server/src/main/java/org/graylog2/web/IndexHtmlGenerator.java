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
