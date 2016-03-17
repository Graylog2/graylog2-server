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
package org.graylog2.periodical;

import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.shared.system.activities.NullActivityWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Provider;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndexRotationThreadTest {
    @Mock
    private Deflector deflector;
    @Mock
    private NotificationService notificationService;
    @Mock
    private Indices indices;
    @Mock
    private Cluster cluster;
    @Mock
    private ClusterConfigService clusterConfigService;

    @Test
    public void testFailedRotation() {
        final Provider<RotationStrategy> provider = new Provider<RotationStrategy>() {
            @Override
            public RotationStrategy get() {
                return new RotationStrategy() {
                    @Override
                    public void rotate() {
                    }

                    @Override
                    public RotationStrategyConfig defaultConfiguration() {
                        return null;
                    }

                    @Override
                    public Class<? extends RotationStrategyConfig> configurationClass() {
                        return null;
                    }
                };
            }
        };

        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(Optional.of(IndexManagementConfig.create("strategy", "retention")));

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                deflector,
                cluster,
                new NullActivityWriter(),
                clusterConfigService,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
        );

        rotationThread.checkForRotation();

        verify(deflector, never()).cycle();
    }

    @Test
    public void testPerformRotation() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = new Provider<RotationStrategy>() {
            @Override
            public RotationStrategy get() {
                return new RotationStrategy() {
                    @Override
                    public void rotate() {
                        deflector.cycle();
                    }

                    @Override
                    public RotationStrategyConfig defaultConfiguration() {
                        return null;
                    }

                    @Override
                    public Class<? extends RotationStrategyConfig> configurationClass() {
                        return null;
                    }
                };
            }
        };

        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(Optional.of(IndexManagementConfig.create("strategy", "retention")));

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                deflector,
                cluster,
                new NullActivityWriter(),
                clusterConfigService,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
        );

        when(deflector.getNewestTargetName()).thenReturn("some_index");

        rotationThread.checkForRotation();

        verify(deflector, times(1)).cycle();
    }

    @Test
    public void testDontPerformRotation() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = new Provider<RotationStrategy>() {
            @Override
            public RotationStrategy get() {
                return new RotationStrategy() {
                    @Override
                    public void rotate() {
                    }

                    @Override
                    public RotationStrategyConfig defaultConfiguration() {
                        return null;
                    }

                    @Override
                    public Class<? extends RotationStrategyConfig> configurationClass() {
                        return null;
                    }
                };
            }
        };

        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(Optional.of(IndexManagementConfig.create("strategy", "retention")));

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                deflector,
                cluster,
                new NullActivityWriter(),
                clusterConfigService,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
        );

        when(deflector.getNewestTargetName()).thenReturn("some_index");

        rotationThread.checkForRotation();

        verify(deflector, never()).cycle();
    }

    @Test
    public void testDontPerformRotationIfClusterIsDown() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = mock(Provider.class);
        when(cluster.isConnected()).thenReturn(false);
        when(cluster.isHealthy()).thenReturn(false);
        when(clusterConfigService.get(IndexManagementConfig.class)).thenReturn(Optional.of(IndexManagementConfig.create("strategy", "retention")));

        final IndexRotationThread rotationThread = new IndexRotationThread(
                notificationService,
                indices,
                deflector,
                cluster,
                new NullActivityWriter(),
                clusterConfigService,
                ImmutableMap.<String, Provider<RotationStrategy>>builder().put("strategy", provider).build()
        );

        rotationThread.doRun();

        verify(deflector, never()).cycle();
        verify(provider, never()).get();
    }
}
