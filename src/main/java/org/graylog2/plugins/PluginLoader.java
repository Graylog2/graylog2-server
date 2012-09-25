/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.plugins;

import com.beust.jcommander.internal.Lists;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.Core;
import org.graylog2.Main;
import org.graylog2.plugin.filters.MessageFilter;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class PluginLoader {
    
    private static final Logger LOG = Logger.getLogger(PluginLoader.class);
    
    private String directory;
    
    public PluginLoader(String directory) {
        this.directory = directory;
    }
    
    public List<Class<? extends MessageFilter>> loadFilterPlugins() {
        List<Class<? extends MessageFilter>> filters = Lists.newArrayList();
        
        try {
            // Load plugins.
            ClassLoader loader = URLClassLoader.newInstance(
                new URL[] { (new java.io.File("plugin/graylog2-example-plugin-filter-otter-1.0.jar")).toURI().toURL() },
                getClass().getClassLoader()
            );
            
            Class<?> clazz = Class.forName("com.example.graylog2examplepluginfilterotter.OtterFilter", true, loader);
            filters.add(clazz.asSubclass(MessageFilter.class));
        } catch (MalformedURLException ex) {
            LOG.error(ex);
   // CONTINUE
        } catch (ClassNotFoundException ex) {
            LOG.error(ex);
   // CONTINUE
        }
        
        return filters;
    }
    
}
