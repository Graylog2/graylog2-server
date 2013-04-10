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

package org.graylog2.plugin.filters;

import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;

/**
 * 
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface MessageFilter {

    /**
     * Process a Message
     *
     * @return true if this message should not further be handled (for example for blacklisting purposes)
     */
    public boolean filter(Message msg, GraylogServer server);

    /**
     * @return The name of this filter. Should not include whitespaces or special characters.
     */
    public String getName();

}
