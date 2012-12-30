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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class PluginLoader<A> {
    
    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private final String baseDirectory;
    private final String subDirectory;
    private final Class<A> type;
    
    public PluginLoader(String baseDirectory, String subDirectory, Class<A> clazz) {
        this.baseDirectory = baseDirectory;
        this.subDirectory = subDirectory;
        this.type = clazz;
    }
    
    public List<A> getPlugins() {
        List<A> plugins = Lists.newArrayList();
        
        // Load all plugin jars.
        for (File jarPath : getAllJars(subDirectory)) {
            try {
                ClassLoader loader = URLClassLoader.newInstance(
                    new URL[] { jarPath.toURI().toURL() },
                    getClass().getClassLoader()
                );

                Class<?> clazz = Class.forName(getClassNameFromJarName(jarPath.getName()), true, loader);
                plugins.add(clazz.asSubclass(type).newInstance());
            } catch (Exception ex) {
                LOG.error("Could not load plugin <" + jarPath.getAbsolutePath() + ">", ex);
                continue;
            }
        }
        
        return plugins;
    }
    
    private List<File> getAllJars(String type) {
        File dir = new File(baseDirectory + "/" + type);
        
        if (!dir.isDirectory()) {
            LOG.error("Plugin path <{}> does not exist or is not a directory.", dir.getAbsolutePath());
            return new ArrayList<File>();
        }
 
        return Arrays.asList(dir.listFiles(new Graylog2PluginFileFilter()));
    }
    
    public static String getClassNameFromJarName(String jar) throws InvalidJarNameException {
        try {
            return jar.split("_gl2plugin.jar")[0];
        } catch(Exception e) {
            LOG.error("Could not extract class name from jar <{}>.", jar);
            throw new InvalidJarNameException("Invalid jar path: " + jar);
        }
    }
    
}
