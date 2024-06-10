package org.graylog2.entitygroups;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.entitygroups.model.Groupable;
import org.graylog2.entitygroups.rest.EntityGroupResource;
import org.graylog2.plugin.PluginModule;

public class EntityGroupsModule extends PluginModule {
    @Override
    protected void configure() {
        addSystemRestResource(EntityGroupResource.class);
    }

    private void addEntityGroupEntityType(String entityTypeName, Class<? extends Groupable> entityClass) {
        entityTypeModelBinder().addBinding(entityTypeName).to(entityClass);
    }

    private MapBinder<String, Groupable> entityTypeModelBinder() {
        return MapBinder.newMapBinder(
                binder(),
                TypeLiteral.get(String.class),
                TypeLiteral.get(Groupable.class)
        );
    }
}
