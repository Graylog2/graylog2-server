/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings;

import com.google.common.base.Strings;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.Configuration;
import org.graylog2.outputs.BlockingBatchedESOutput;
import org.graylog2.outputs.DefaultMessageOutput;
import org.graylog2.outputs.GelfOutput;
import org.graylog2.outputs.LoggingOutput;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MessageOutputBindings extends Graylog2Module {
    private static final Logger LOG = LoggerFactory.getLogger(MessageOutputBindings.class);

    private final Configuration configuration;
    private final ChainingClassLoader chainingClassLoader;

    public MessageOutputBindings(final Configuration configuration, ChainingClassLoader chainingClassLoader) {
        this.configuration = configuration;
        this.chainingClassLoader = chainingClassLoader;
    }

    @Override
    protected void configure() {
        final Class<? extends MessageOutput> defaultMessageOutputClass = getDefaultMessageOutputClass(BlockingBatchedESOutput.class);
        LOG.debug("Using default message output class: {}", defaultMessageOutputClass.getCanonicalName());
        bind(MessageOutput.class).annotatedWith(DefaultMessageOutput.class).to(defaultMessageOutputClass).in(Scopes.SINGLETON);

        final MapBinder<String, MessageOutput.Factory<? extends MessageOutput>> outputMapBinder = outputsMapBinder();
        installOutput(outputMapBinder, GelfOutput.class, GelfOutput.Factory.class);
        installOutput(outputMapBinder, LoggingOutput.class, LoggingOutput.Factory.class);
    }

    private Class<? extends MessageOutput> getDefaultMessageOutputClass(Class<? extends MessageOutput> fallbackClass) {
        // Just use the default fallback if nothing is configured. This is the default case.
        if (Strings.isNullOrEmpty(configuration.getDefaultMessageOutputClass())) {
            return fallbackClass;
        }

        try {
            @SuppressWarnings("unchecked")
            final Class<? extends MessageOutput> defaultMessageOutputClass = (Class<? extends MessageOutput>) chainingClassLoader.loadClass(configuration.getDefaultMessageOutputClass());

            if (MessageOutput.class.isAssignableFrom(defaultMessageOutputClass)) {
                LOG.info("Using {} as default message output", defaultMessageOutputClass.getCanonicalName());
                return defaultMessageOutputClass;
            } else {
                LOG.warn("Class \"{}\" is not a subclass of \"{}\". Using \"{}\" as default message output",
                        configuration.getDefaultMessageOutputClass(),
                        MessageOutput.class.getCanonicalName(),
                        fallbackClass.getCanonicalName());
                return fallbackClass;
            }
        } catch (ClassNotFoundException e) {
            LOG.warn("Unable to find default message output class \"{}\", using \"{}\"",
                    configuration.getDefaultMessageOutputClass(),
                    fallbackClass.getCanonicalName());
            return fallbackClass;
        }
    }
}
