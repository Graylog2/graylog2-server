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

package org.graylog2.plugin.logmessage;

import java.util.List;
import java.util.Map;
import org.graylog2.plugin.streams.Stream;
//import org.graylog2.streams.Stream;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface LogMessage {

    public boolean isComplete();

    public String getId();
    
    @Override
    public String toString();

    public double getCreatedAt();

    public void setCreatedAt(double createdAt);

    public String getFacility();

    public void setFacility(String facility);

    public String getFile();

    public void setFile(String file);

    public String getFullMessage();

    public void setFullMessage(String fullMessage);

    public String getHost();

    public void setHost(String host);
    
    public int getLevel();

    public void setLevel(int level);

    public int getLine();

    public void setLine(int line);

    public String getShortMessage();

    public void setShortMessage(String shortMessage);

    public void addAdditionalData(String key, Object value);
    
    public void addAdditionalData(String key, String value);

    public void addAdditionalData(Map<String, String> fields);

    public void removeAdditionalData(String key);

    public Map<String, Object> getAdditionalData();

    public void setStreams(List<Stream> streams);

    public List<Stream> getStreams();
    
    public Map<String, Object> toElasticSearchObject();
    
}
