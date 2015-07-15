package org.graylog.plugins.netflow;

import org.graylog.plugins.netflow.codecs.NetflowCodec;
import org.graylog.plugins.netflow.inputs.NetflowInput;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

/**
 * Extend the PluginModule abstract class here to add you plugin to the system.
 */
public class NetflowPluginModule extends PluginModule {
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
        addMessageInput(NetflowInput.class);
        addCodec("netflow", NetflowCodec.class);
    }
}
