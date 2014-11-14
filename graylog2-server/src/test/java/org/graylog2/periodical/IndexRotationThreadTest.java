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
package org.graylog2.periodical;

import com.google.inject.Provider;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.initializers.IndexerSetupService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import org.testng.annotations.Test;

import javax.annotation.Nullable;

import static org.mockito.Mockito.*;

public class IndexRotationThreadTest {

    @Test
    public void testFailedRotation() {
        final Provider<RotationStrategy> provider = new Provider<RotationStrategy>() {
            @Override
            public RotationStrategy get() {
                return new RotationStrategy() {
                    @Nullable
                    @Override
                    public Result shouldRotate(String index) {
                        return null;
                    }
                };
            }
        };

        final Deflector deflector = mock(Deflector.class);
        final IndexRotationThread rotationThread = new IndexRotationThread(
                mock(NotificationService.class),
                mock(Indices.class),
                deflector,
                mock(SystemMessageActivityWriter.class),
                mock(IndexerSetupService.class),
                provider
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
                    @Nullable
                    @Override
                    public Result shouldRotate(String index) {
                        return new Result() {
                            @Override
                            public String getDescription() {
                                return "performed";
                            }

                            @Override
                            public boolean shouldRotate() {
                                return true;
                            }
                        };
                    }
                };
            }
        };

        final Deflector deflector = mock(Deflector.class);
        final IndexRotationThread rotationThread = new IndexRotationThread(
                mock(NotificationService.class),
                mock(Indices.class),
                deflector,
                mock(SystemMessageActivityWriter.class),
                mock(IndexerSetupService.class),
                provider
            );

        when(deflector.getNewestTargetName()).thenReturn("some_index");

        rotationThread.checkForRotation();

        verify(deflector, times(1)).cycle();
        verify(deflector, times(1)).getNewestTargetName();
    }

    @Test
    public void testDontPerformRotation() throws NoTargetIndexException {
        final Provider<RotationStrategy> provider = new Provider<RotationStrategy>() {
            @Override
            public RotationStrategy get() {
                return new RotationStrategy() {
                    @Nullable
                    @Override
                    public Result shouldRotate(String index) {
                        return new Result() {
                            @Override
                            public String getDescription() {
                                return "performed";
                            }

                            @Override
                            public boolean shouldRotate() {
                                return false;
                            }
                        };
                    }
                };
            }
        };

        final Deflector deflector = mock(Deflector.class);
        final IndexRotationThread rotationThread = new IndexRotationThread(
                mock(NotificationService.class),
                mock(Indices.class),
                deflector,
                mock(SystemMessageActivityWriter.class),
                mock(IndexerSetupService.class),
                provider
        );

        when(deflector.getNewestTargetName()).thenReturn("some_index");

        rotationThread.checkForRotation();

        verify(deflector, never()).cycle();
        verify(deflector, times(1)).getNewestTargetName();
    }
}