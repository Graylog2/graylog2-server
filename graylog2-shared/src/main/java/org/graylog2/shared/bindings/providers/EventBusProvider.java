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
package org.graylog2.shared.bindings.providers;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.BaseConfiguration;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.concurrent.Executors;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class EventBusProvider implements Provider<EventBus> {
    private final BaseConfiguration configuration;

    @Inject
    public EventBusProvider(BaseConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public EventBus get() {
        return new AsyncEventBus("graylog2-eventbus",
                Executors.newFixedThreadPool(
                        configuration.getAsyncEventbusProcessors(),
                        new ThreadFactoryBuilder()
                                .setDaemon(true)
                                .setNameFormat(
                                        "eventbus-handler-%d")
                                .build()
                ));
    }
}
