package org.graylog2.entitygroups;

import org.graylog2.entitygroups.contentpacks.EntityGroupFacade;
import org.graylog2.entitygroups.entities.GroupableStream;
import org.graylog2.entitygroups.rest.EntityGroupResource;
import org.graylog2.plugin.PluginModule;

public class EntityGroupsModule extends PluginModule {
    @Override
    protected void configure() {
        addSystemRestResource(EntityGroupResource.class);
        addEntityFacade(EntityGroupFacade.TYPE_V1, EntityGroupFacade.class);
        addEntityGroupEntityType(GroupableStream.TYPE_NAME, GroupableStream.class);
    }
}
