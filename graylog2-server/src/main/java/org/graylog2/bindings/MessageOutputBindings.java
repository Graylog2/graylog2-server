package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.outputs.BatchedElasticSearchOutput;
import org.graylog2.outputs.DefaultMessageOutput;
import org.graylog2.outputs.GelfOutput;
import org.graylog2.outputs.LoggingOutput;
import org.graylog2.plugin.outputs.MessageOutput;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageOutputBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(MessageOutput.class).annotatedWith(DefaultMessageOutput.class).to(BatchedElasticSearchOutput.class).in(Scopes.SINGLETON);

        TypeLiteral<Class<? extends MessageOutput>> typeLiteral = new TypeLiteral<Class<? extends MessageOutput>>(){};
        Multibinder<Class<? extends MessageOutput>> messageOutputs = Multibinder.newSetBinder(binder(), typeLiteral);

        messageOutputs.addBinding().toInstance(LoggingOutput.class);
        messageOutputs.addBinding().toInstance(GelfOutput.class);
    }
}
