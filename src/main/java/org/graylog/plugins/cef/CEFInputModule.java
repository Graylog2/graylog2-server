package org.graylog.plugins.cef;

import org.graylog.plugins.cef.codec.CEFCodec;
import org.graylog.plugins.cef.input.CEFTCPInput;
import org.graylog.plugins.cef.input.CEFUDPInput;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class CEFInputModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        addCodec(CEFCodec.NAME, CEFCodec.class);

        addMessageInput(CEFUDPInput.class);
        addMessageInput(CEFTCPInput.class);
    }
}
