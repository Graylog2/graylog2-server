package org.graylog2.datatier;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog2.datatier.common.DataTierRotation;
import org.graylog2.datatier.open.OpenDataTierOrchestrator;

public class DataTierBindings extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(DataTierRotation.Factory.class));
        OptionalBinder.newOptionalBinder(binder(), DataTierOrchestrator.class).setDefault().to(OpenDataTierOrchestrator.class);
    }

}
