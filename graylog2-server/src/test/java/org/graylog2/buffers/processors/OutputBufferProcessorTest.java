/**
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
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.buffers.processors;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 *
 * @author lennart.koopmann
 */
public class OutputBufferProcessorTest {
    
    @Test(enabled = false)
    // Ignored until FakeStream class has been restored
    public void testOnEventWritesToAllOutputsWhenGettingBatches() throws Exception {
        /*FakeOutput fo1 = new FakeOutput();
        FakeOutput fo2 = new FakeOutput();
        
        GraylogServerStub server = new GraylogServerStub();
        server.registerOutput(fo1);
        server.registerOutput(fo2);
        
        OutputBufferProcessor proc = new OutputBufferProcessor(server, 0, 1);
        
        FakeStream stream1 = new FakeStream("fakestream1");
        stream1.addOutput(fo1);
        stream1.addOutput(fo2);
        
        FakeStream stream2 = new FakeStream("fakestream2");
        stream1.addOutput(fo2);

        List<Stream> streamList1 = Lists.newArrayList();
        streamList1.add(stream1);
        streamList1.add(stream2);
        
        List<Stream> streamList2 = Lists.newArrayList();
        streamList2.add(stream1);

        List<Stream> streamList3 = Lists.newArrayList();
        streamList3.add(stream2);
        
        Message msg1 = TestHelper.simpleLogMessage();
        msg1.setStreams(streamList1);
        
        Message msg2 = TestHelper.simpleLogMessage();
        msg2.setStreams(streamList2);
        
        Message msg3 = TestHelper.simpleLogMessage();
        msg3.setStreams(streamList3);
        
        MessageEvent e1 = new MessageEvent();
        e1.setMessage(msg1);
        
        MessageEvent e2 = new MessageEvent();
        e2.setMessage(msg2);
        
        MessageEvent e3 = new MessageEvent();
        e3.setMessage(msg3);
        
        proc.onEvent(e1, 1, false);
        proc.onEvent(e2, 2, false);
        proc.onEvent(e3, 3, true);
        
        assertEquals(1, fo1.getCallCount());
        assertEquals(1, fo2.getCallCount());

        assertEquals(2, fo1.getWriteCount());
        assertEquals(2, fo2.getWriteCount());*/
    }
}
