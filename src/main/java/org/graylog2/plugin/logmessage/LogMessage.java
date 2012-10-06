/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/

package org.graylog2.plugin.logmessage;

import java.util.List;
import java.util.Map;
import org.graylog2.plugin.streams.Stream;

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
