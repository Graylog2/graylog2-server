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
package org.graylog2.indexer.esplugin;

import com.google.common.eventbus.EventBus;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;

import java.util.EnumSet;
import java.util.List;

import static org.elasticsearch.common.base.Preconditions.checkNotNull;

public class IndexChangeMonitor extends AbstractLifecycleComponent<IndexChangeMonitor> implements ClusterStateListener {
    private static final EnumSet<ClusterState.ClusterStateStatus> VALID_CLUSTER_STATES =
            EnumSet.of(ClusterState.ClusterStateStatus.BEING_APPLIED, ClusterState.ClusterStateStatus.APPLIED);

    // Yes, this sucks, but ES and Graylog use different injectors and it's not obvious how to bridge them, so I'm using a static. Shoot me.
    private static EventBus eventBus = null;

    private final ClusterService clusterService;

    @org.elasticsearch.common.inject.Inject
    public IndexChangeMonitor(Settings settings, ClusterService clusterService) {
        super(settings);
        this.clusterService = checkNotNull(clusterService);
    }

    public static void setEventBus(EventBus eventBus) {
        IndexChangeMonitor.eventBus = eventBus;
    }

    @Override
    public void clusterChanged(ClusterChangedEvent event) {
        if (!VALID_CLUSTER_STATES.contains(event.state().status())) {
            // Only process fully applied cluster states
            return;
        }

        if (eventBus != null) {
            final List<String> indicesDeleted = event.indicesDeleted();
            if (!indicesDeleted.isEmpty()) {
                eventBus.post(IndicesDeletedEvent.create(indicesDeleted));
            }
        }
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        clusterService.add(this);
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        clusterService.remove(this);
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }
}
