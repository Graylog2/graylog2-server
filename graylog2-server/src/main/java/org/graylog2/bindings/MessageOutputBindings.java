/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
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
