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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    public static final String FILTER_DIR = "filters";
    
    private String baseDirectory;
    
    public PluginLoader(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }
    
    public List<Class<? extends MessageFilter>> loadFilterPlugins() {
        List<Class<? extends MessageFilter>> filters = Lists.newArrayList();
        
        // Load all plugin jars.
        for (File jarPath : getAllJars(FILTER_DIR)) {
            try {
                ClassLoader loader = URLClassLoader.newInstance(
                    new URL[] { jarPath.toURI().toURL() },
                    getClass().getClassLoader()
                );

                Class<?> clazz = Class.forName(getClassNameFromJarName(jarPath.getName()), true, loader);
                filters.add(clazz.asSubclass(MessageFilter.class));
            } catch (MalformedURLException ex) {
                LOG.error("Could not load plugin <" + jarPath.getAbsolutePath() + ">", ex);
                continue;
            } catch (ClassNotFoundException ex) {
                LOG.error("Could not load plugin <" + jarPath.getAbsolutePath() + ">", ex);
                continue;
            } catch (InvalidJarNameException ex) {
                LOG.error("Could not load plugin <" + jarPath.getAbsolutePath() + ">", ex);
                continue;
            }
        }
        
        return filters;
    }
    
    private List<File> getAllJars(String type) {
        File dir = new File(baseDirectory + "/" + type);
        
        if (!dir.isDirectory()) {
            LOG.error("Plugin path <" + dir.getAbsolutePath() + "> does not exist or is not a directory.");
            return new ArrayList<File>();
        }
 
        return Arrays.asList(dir.listFiles(new Graylog2PluginFileFilter()));
    }
    
    private String getClassNameFromJarName(String jar) throws InvalidJarNameException {
        try {
            return jar.split("_gl2plugin.jar")[0];
        } catch(Exception e) {
            LOG.error("Could not extract class name from jar <" + jar + ">.");
            throw new InvalidJarNameException("Invalid jar path: " + jar);
        }
    }
    
}
