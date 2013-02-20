/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.buffers.processors;

import com.mongodb.BasicDBObject;
import java.util.List;
import org.bson.BSON;
import org.bson.types.ObjectId;
import org.elasticsearch.common.collect.Lists;
import org.graylog2.GraylogServerStub;
import org.graylog2.TestHelper;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.buffers.processors.fakeoutputs.FakeOutput;
import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.FakeStream;
import org.graylog2.streams.StreamImpl;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class OutputBufferProcessorTest {
    
    @Test
    public void testOnEventWritesToAllOutputsWhenGettingBatches() throws Exception {
        FakeOutput fo1 = new FakeOutput();
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
        
        LogMessage msg1 = TestHelper.simpleLogMessage();
        msg1.setStreams(streamList1);
        
        LogMessage msg2 = TestHelper.simpleLogMessage();
        msg2.setStreams(streamList2);
        
        LogMessage msg3 = TestHelper.simpleLogMessage();
        msg3.setStreams(streamList3);
        
        LogMessageEvent e1 = new LogMessageEvent();
        e1.setMessage(msg1);
        
        LogMessageEvent e2 = new LogMessageEvent();
        e2.setMessage(msg2);
        
        LogMessageEvent e3 = new LogMessageEvent();
        e3.setMessage(msg3);
        
        proc.onEvent(e1, 1, false);
        proc.onEvent(e2, 2, false);
        proc.onEvent(e3, 3, true);
        
        assertEquals(1, fo1.getCallCount());
        assertEquals(1, fo2.getCallCount());

        assertEquals(2, fo1.getWriteCount());
        assertEquals(2, fo2.getWriteCount());
    }
}
