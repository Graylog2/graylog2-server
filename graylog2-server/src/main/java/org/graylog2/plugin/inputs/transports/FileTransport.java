package org.graylog2.plugin.inputs.transports;

import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;

public class FileTransport extends GeneratorTransport {

    static final String custom_field_def_dest_file_path = "custom_field_def_dest_file_path";
    static final String custom_field_def_filename = "custom_field_def_filename";


    public FileTransport(EventBus eventBus, Configuration configuration) {

        super(eventBus, configuration);
    }

    @Override
    protected RawMessage produceRawMessage(MessageInput input) {
        return null;
    }


}
