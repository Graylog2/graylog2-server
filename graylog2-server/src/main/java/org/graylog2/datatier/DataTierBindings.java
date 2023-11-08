package org.graylog2.datatier;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.datatier.tier.DataTierValidator;
import org.graylog2.datatier.tier.hot.HotTierValidator;

public class DataTierBindings extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<DataTierValidator> validatorBinder = Multibinder.newSetBinder(binder(), DataTierValidator.class);
        validatorBinder.addBinding().to(HotTierValidator.class);
    }

}
