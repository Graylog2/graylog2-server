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
package org.graylog2.events;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;

public class ClusterEventBus extends AsyncEventBus {
    public ClusterEventBus () {
        this(MoreExecutors.directExecutor());
    }

    public ClusterEventBus(Executor executor) {
        super(executor);
    }

    public ClusterEventBus(Executor executor, SubscriberExceptionHandler subscriberExceptionHandler) {
        super(executor, subscriberExceptionHandler);
    }

    public ClusterEventBus(String identifier, Executor executor) {
        super(identifier, executor);
    }

    @Override
    public void register(Object object) {
        throw new IllegalStateException("Do not use ClusterEventBus for regular subscriptions. You probably want to use the regular EventBus.");
    }

    /**
     * Only use this if you maintain the cluster event bus! Use regular EventBus to receive cluster event updates.
     * @param object
     */
    public void registerClusterEventSubscriber(Object object) {
        super.register(object);
    }
}
