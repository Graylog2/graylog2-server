package org.graylog2.periodical;

import com.google.inject.Provider;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.initializers.IndexerSetupService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.system.activities.ActivityWriter;
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
                mock(ActivityWriter.class),
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
                mock(ActivityWriter.class),
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
                mock(ActivityWriter.class),
                mock(IndexerSetupService.class),
                provider
        );

        when(deflector.getNewestTargetName()).thenReturn("some_index");

        rotationThread.checkForRotation();

        verify(deflector, never()).cycle();
        verify(deflector, times(1)).getNewestTargetName();
    }
}