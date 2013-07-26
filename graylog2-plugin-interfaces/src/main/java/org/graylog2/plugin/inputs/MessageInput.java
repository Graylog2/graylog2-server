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
package org.graylog2.plugin.inputs;

import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public abstract class MessageInput {

    protected String inputId;
    protected String persistId;

    public abstract void configure(Configuration config, GraylogServer graylogServer) throws ConfigurationException;

    public abstract void launch() throws MisfireException;
    public abstract void stop();

    public abstract ConfigurationRequest getRequestedConfiguration();

    public abstract boolean isExclusive();
    public abstract String getName();
    public abstract Map<String, Object> getAttributes();

    public void setId(String id) {
        this.inputId = id;
    }

    public void setPersistId(String id) {
        this.persistId = id;
    }

    public String getId() {
        return inputId;
    }

    public String getPersistId() {
        return persistId;
    }
    
}