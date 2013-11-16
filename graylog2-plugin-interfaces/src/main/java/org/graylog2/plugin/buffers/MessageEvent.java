/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@torch.sh>
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

package org.graylog2.plugin.buffers;

import com.lmax.disruptor.EventFactory;
import org.graylog2.plugin.Message;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class MessageEvent {

    private Message msg;
    
    public Message getMessage()
    {
        return msg;
    }

    public void setMessage(final Message msg)
    {
        this.msg = msg;
    }

    public final static EventFactory<MessageEvent> EVENT_FACTORY = new EventFactory<MessageEvent>()
    {
        @Override
        public MessageEvent newInstance()
        {
            return new MessageEvent();
        }
    };

}
