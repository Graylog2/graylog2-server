package org.graylog2.bindings.providers;

import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.system.jobs.SystemJobFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SystemJobFactoryProvider implements Provider<SystemJobFactory> {
    private static SystemJobFactory systemJobFactory = null;

    @Inject
    public SystemJobFactoryProvider(FixDeflectorByDeleteJob.Factory deleteJobFactory,
                                    FixDeflectorByMoveJob.Factory moveJobFactory) {
        if (systemJobFactory == null)
            systemJobFactory = new SystemJobFactory(moveJobFactory, deleteJobFactory);
    }

    @Override
    public SystemJobFactory get() {
        return systemJobFactory;
    }
}
