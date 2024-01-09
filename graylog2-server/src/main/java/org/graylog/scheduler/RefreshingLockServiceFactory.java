package org.graylog.scheduler;

import org.graylog2.cluster.lock.LockService;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

public class RefreshingLockServiceFactory {
    private final LockService lockService;
    private final ScheduledExecutorService scheduler;
    private final Duration leaderElectionLockTTL;

    @Inject
    public RefreshingLockServiceFactory(LockService lockService,
                                        @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                        @Named("lock_service_lock_ttl") Duration leaderElectionLockTTL) {

        this.lockService = lockService;
        this.scheduler = scheduler;
        this.leaderElectionLockTTL = leaderElectionLockTTL;
    }

    public RefreshingLockService create() {
        return new RefreshingLockService(lockService, scheduler, leaderElectionLockTTL);
    }
}
