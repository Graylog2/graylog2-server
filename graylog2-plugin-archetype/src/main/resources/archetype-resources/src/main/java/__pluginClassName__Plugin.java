package ${package};

import java.util.Collection;
import com.google.common.collect.Lists;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginModule;


/* Implement the Plugin interface here. */

public class ${pluginClassName}Plugin implements Plugin {
    @Override
    public Collection<PluginModule> modules () {
        return Lists.newArrayList((PluginModule) new ${pluginClassName}Module());
    }
}
