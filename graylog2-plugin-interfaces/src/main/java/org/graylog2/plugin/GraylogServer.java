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
package org.graylog2.plugin;

import java.util.Map;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.indexer.MessageGateway;
import org.graylog2.plugin.streams.Stream;

/**
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface GraylogServer extends Runnable {

    public Buffer getOutputBuffer();
    
    public boolean isMaster();
    
    public String getNodeId();
    
    public MessageGateway getMessageGateway();
    
    public Map<String, Stream> getEnabledStreams();

    public MetricRegistry metrics();

    void deleteIndexShortcut(String indexName);

    void closeIndexShortcut(String indexName);
}
