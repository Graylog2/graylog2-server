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
package org.graylog2.messageprocessors;

import com.google.inject.Scopes;
import org.graylog2.plugin.PluginModule;

public class MessageProcessorModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageProcessor(MessageFilterChainProcessor.class, MessageFilterChainProcessor.Descriptor.class);
        // must not be a singleton, because each thread should get an isolated copy of the processors
        bind(OrderedMessageProcessors.class).in(Scopes.NO_SCOPE);
    }

}
