package org.graylog.plugins.netflow;

import org.graylog.plugins.netflow.codecs.NetFlowCodec;
import org.graylog.plugins.netflow.inputs.NetFlowInput;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

/**
 * Extend the PluginModule abstract class here to add you plugin to the system.
 */
public class NetFlowPluginModule extends PluginModule {
    /**
     * Returns all configuration beans required by this plugin.
     *
     * Implementing this method is optional. The default method returns an empty {@link Set}.
     */
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        addMessageInput(NetFlowInput.class);
        addCodec("netflow", NetFlowCodec.class);
    }
}
