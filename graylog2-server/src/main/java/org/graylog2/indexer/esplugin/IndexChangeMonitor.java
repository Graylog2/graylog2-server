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

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.ClusterChangedEvent;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ClusterStateListener;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.settings.Settings;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

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
            final Set<String> indicesDeleted = new HashSet<>(event.indicesDeleted());
            if (!indicesDeleted.isEmpty()) {
                eventBus.post(IndicesDeletedEvent.create(indicesDeleted));
            }

            final Set<String> indicesClosed = calculateClosedIndices(event.state(), event.previousState());
            if (!indicesClosed.isEmpty()) {
                eventBus.post(IndicesClosedEvent.create(indicesClosed));
            }

            final Set<String> indicesReopened = calculateReopenedIndices(event.state(), event.previousState());
            if (!indicesReopened.isEmpty()) {
                eventBus.post(IndicesReopenedEvent.create(indicesReopened));
            }
        }
    }

    private Set<String> calculateClosedIndices(ClusterState currentState, @Nullable ClusterState previousState) {
        if (previousState == null || previousState.metaData() == currentState.metaData()) {
            return Collections.emptySet();
        }

        final Set<String> currentClosedIndices = getClosedIndices(currentState.getMetaData());
        final Set<String> previousClosedIndices = getClosedIndices(previousState.getMetaData());

        return Sets.difference(currentClosedIndices, previousClosedIndices);
    }

    private Set<String> calculateReopenedIndices(ClusterState currentState, @Nullable ClusterState previousState) {
        if (previousState == null || previousState.metaData() == currentState.metaData()) {
            return Collections.emptySet();
        }

        final Set<String> currentClosedIndices = getClosedIndices(currentState.getMetaData());
        final Set<String> previousClosedIndices = getClosedIndices(previousState.getMetaData());

        return Sets.difference(previousClosedIndices, currentClosedIndices);
    }

    private HashSet<String> getClosedIndices(MetaData currentMetaData) {
        return new HashSet<>(Arrays.asList(currentMetaData.concreteAllClosedIndices()));
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
