package test.restresource;

import org.graylog2.plugin.PluginModule;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class TestResourceModule extends PluginModule {
    @Override
    protected void configure() {
        addRestResource(TestResource.class);
    }
}
