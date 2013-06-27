/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.buffers.processors;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author lennart.koopmann
 */
public class OutputBufferProcessorTest {
    
    @Test
    @Ignore("Ignored until FakeStream class has been restored")
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
