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

package org.graylog2.indexer.rotation.strategies;

import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageCountRotationStrategyTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private Deflector deflector;

    @Mock
    private Indices indices;

    @Test
    public void testRotate() throws Exception {
        when(indices.numberOfMessages("name")).thenReturn(10L);
        when(deflector.getNewestTargetName()).thenReturn("name");
        when(clusterConfigService.get(MessageCountRotationStrategyConfig.class)).thenReturn(Optional.of(MessageCountRotationStrategyConfig.create(5)));

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(indices, deflector, clusterConfigService);

        strategy.rotate();
        verify(deflector, times(1)).cycle();
        reset(deflector);
    }

    @Test
    public void testDontRotate() throws Exception {
        when(indices.numberOfMessages("name")).thenReturn(1L);
        when(deflector.getNewestTargetName()).thenReturn("name");
        when(clusterConfigService.get(MessageCountRotationStrategyConfig.class)).thenReturn(Optional.of(MessageCountRotationStrategyConfig.create(5)));

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(indices, deflector, clusterConfigService);

        strategy.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);
    }


    @Test
    public void testIndexUnavailable() throws Exception {
        doThrow(IndexNotFoundException.class).when(indices).numberOfMessages("name");
        when(deflector.getNewestTargetName()).thenReturn("name");
        when(clusterConfigService.get(MessageCountRotationStrategyConfig.class)).thenReturn(Optional.of(MessageCountRotationStrategyConfig.create(5)));

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(indices, deflector, clusterConfigService);

        strategy.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);
    }
}
