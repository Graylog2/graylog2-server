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
package org.graylog2.system.information;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import com.google.common.collect.Lists;
import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 *  @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Sender {
    
    private static final Logger LOG = LoggerFactory.getLogger(Sender.class);
    
    public final static String TARGET = "http://systemstats.graylog2.org/";
    public final static String LOCAL_TARGET = "http://localhost:9393/";
    
    public static void send(Map<String, Object> info, Core server) {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient httpClient = new DefaultHttpClient();

        try {
            HttpPost request = new HttpPost(getTarget(server));

            List<NameValuePair> nameValuePairs = Lists.newArrayList();
            nameValuePairs.add(new BasicNameValuePair("systeminfo", objectMapper.writeValueAsString(info)));
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            
            HttpResponse response = httpClient.execute(request);
            
            if (response.getStatusLine().getStatusCode() != 201) {
                LOG.warn("Response code for system statistics was not 201, but " + response.getStatusLine().getStatusCode());
            }
        }catch (Exception e) {
            LOG.warn("Could not send system statistics.", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    private static String getTarget(Core server) {
        return server.isLocalMode() ? LOCAL_TARGET : TARGET;
    }
    
}
