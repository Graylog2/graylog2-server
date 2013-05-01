package org.graylog2.buffers.processors.fakestreams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.GraylogServerStub;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.streams.StreamImpl;

import java.util.List;

public class FakeStream extends StreamImpl {
    private List<MessageOutput> outputs = Lists.newArrayList();

    public FakeStream(String title) {
        super(Maps.<String, Object>newHashMap(), new GraylogServerStub());
    }

    public void addOutput(MessageOutput output) {
        outputs.add(output);
    }
}