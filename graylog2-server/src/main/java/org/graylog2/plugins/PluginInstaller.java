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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class PluginInstaller {
    
    // seed mongodb
    // ask user to configure plugin and restart graylog2-server
    
    private final static String API_TARGET = "http://plugins.graylog2.org/plugin";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String shortname;
    private final String version;
    private final boolean force;
    
    /*
     * those are set in getRequestedConfiguration which is a bit unfortunate,
     * because they cannot be built explicitly and are only set when 
     * getRequestedConfiguration has been called. something for a second iteration.
     */
    private String pluginClassName;
    private String pluginName;
    
    public PluginInstaller(String shortname, String version, boolean force) {
        this.shortname = shortname;
        this.version = version;
        this.force = force;
    }
    
    public void install() {
        System.out.println("Attempting to install plugin <" + shortname + "> version <" + version + ">.");
        
        if (force) {
            System.out.println("In force mode. Even installing if not officially"
                    + " compatible to this version of graylog2-server.");
        }
        
        try {
            PluginApiResponse pluginInformation = getPluginInformation();

            System.out.println("Got plugin information from API.");

            if (!force && !compatible(pluginInformation.compatible_versions)) {
                System.out.println("Plugin is not officially compatible to this version "
                        + "of graylog2-server. Run with --force-plugin to force installation.");
                return;
            }
            
            System.out.println("Downloading JAR: " + pluginInformation.jar);
            downloadAndCopyJar(pluginInformation.jar, pluginInformation.getPluginTypeName());
            String jarPath = jarPath(pluginInformation.jar, pluginInformation.getPluginTypeName());
            System.out.println("Copied JAR to " + jarPath);

            System.out.println("All done. Please restart graylog2-server to load the plugin.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private PluginApiResponse getPluginInformation() throws Exception {
        PluginApiResponse result;
        
        HttpURLConnection connection = null;

        try {
            URL endpoint = new URL(buildUrl());
            connection = (HttpURLConnection) endpoint.openConnection();

            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Got HTTP response code "
                        + connection.getResponseCode() + " "
                        + connection.getResponseMessage() + ". Expected HTTP 200.");
            }
            
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            
            result = objectMapper.readValue(rd.readLine(), PluginApiResponse.class);
        } finally {
            // Make sure to close connection.
            if(connection != null) {
                connection.disconnect();
            }
        }
        
        return result;
    }
    
    private void downloadAndCopyJar(String url, String pluginType) throws Exception {        
        int startTime = Tools.getUTCTimestamp();

        URL jar = new URL(url);
        jar.openConnection();
        InputStream reader = jar.openStream();

        FileOutputStream writer = new FileOutputStream(jarPath(url, pluginType));
        byte[] buffer = new byte[153600];
        int totalBytesRead = 0;
        int bytesRead = 0;

        while ((bytesRead = reader.read(buffer)) > 0) {  
            writer.write(buffer, 0, bytesRead);
            buffer = new byte[153600];
            totalBytesRead += bytesRead;
        }

        long endTime = Tools.getUTCTimestamp();

        System.out.println("Done. " + totalBytesRead + " bytes read "
                + "(took " + (endTime - startTime) + "s).");
        writer.close();
        reader.close();
    }
    
    public static boolean compatible(Set<String> versions) {
        if (versions == null) {
            return false;
        }
        
        return versions.contains(Core.GRAYLOG2_VERSION);
    }
    
    public static String jarPath(String jarUrl, String pluginType) {
        try {
            String path = "plugin/" + pluginType + "/" + jarUrl.substring(jarUrl.lastIndexOf("/")+1);
            
            // lol just to make sure...
            if (path.startsWith("/")) {
                throw new RuntimeException("Extracted JAR path starts with /. This should never happen.");
            }
            
            return path;
        } catch(Exception e) {
            throw new RuntimeException("Could not build JAR path.");
        }
    }
    
    private String buildUrl() {
        return API_TARGET + "/" + shortname + "/" + version;
    }
    
}
