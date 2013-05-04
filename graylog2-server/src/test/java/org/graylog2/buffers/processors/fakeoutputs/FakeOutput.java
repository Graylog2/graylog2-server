/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.buffers.processors.fakeoutputs;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;

/**
 *
 * @author lennart.koopmann
 */
public class FakeOutput implements MessageOutput {

    private int callCount = 0;
    private int writeCount = 0;
    
    @Override
    public void initialize(Map<String, String> config) throws MessageOutputConfigurationException {
    }

    @Override
    public void write(List<Message> messages, OutputStreamConfiguration streamConfiguration, GraylogServer server) throws Exception {
        this.callCount++;
        this.writeCount += messages.size();
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        return Maps.newHashMap();
    }

    @Override
    public Map<String, String> getRequestedStreamConfiguration() {
        return Maps.newHashMap();
    }

    @Override
    public String getName() {
        return "FAKE OUTPUT";
    }
    
    public int getCallCount() {
        return callCount;
    }
    
    public int getWriteCount() {
        return writeCount;
    }
    
}
