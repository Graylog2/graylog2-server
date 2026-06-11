/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.datanode.restart;

import com.github.oxo42.stateless4j.StateMachine;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.datanode.dto.ClusterState;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.indexer.indices.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Singleton
public class RollingRestartService {
    private static final Logger LOG = LoggerFactory.getLogger(RollingRestartService.class);

    static final Duration GREEN_WAIT_TIMEOUT = Duration.ofMinutes(30);
    static final int MIN_DATANODES = 3;

    private final RollingRestartJobService jobService;
    private final RollingRestartActions actions;

    @Inject
    public RollingRestartService(RollingRestartJobService jobService, RollingRestartActions actions) {
        this.jobService = jobService;
        this.actions = actions;
    }

    public RollingRestartJob start(String triggeredBy, boolean force) {
        final List<String> failures = checkPreconditions(force);
        if (!failures.isEmpty()) {
            throw new RollingRestartPreconditionsException(failures);
        }

        final List<RollingRestartNodeEntry> nodes = actions.liveDataNodes().stream()
                .sorted(Comparator.comparing(DataNodeDto::getHostname))
                .map(n -> RollingRestartNodeEntry.pending(n.getHostname(), n.getNodeId()))
                .toList();

        final Instant now = Instant.now();
        final RollingRestartJob job = new RollingRestartJob(
                null,
                now,
                now,
                now,
                null,
                RollingRestartJobStatus.ACTIVE,
                RollingRestartState.PREPARING_CLUSTER,
                null,
                null,
                triggeredBy,
                false,
                -1,
                nodes);
        final RollingRestartJob saved = jobService.save(job);
        LOG.info("Started rolling restart job {} triggered by {} ({} nodes)", saved.id(), triggeredBy, nodes.size());
        return saved;
    }

    private List<String> checkPreconditions(boolean force) {
        final List<String> failures = new ArrayList<>();
        if (jobService.findActive().isPresent()) {
            failures.add("Another rolling restart job is already active or paused");
        }
        final List<DataNodeDto> dataNodes = actions.liveDataNodes();
        if (dataNodes.size() < MIN_DATANODES) {
            failures.add("Need at least " + MIN_DATANODES + " DataNodes for safe rolling restart (found " + dataNodes.size() + ")");
        }
        try {
            final ClusterState state = actions.getClusterState();
            if (state.status() != HealthStatus.Green && !force) {
                failures.add("Cluster status is " + state.status() + " — must be GREEN (pass force=true to override)");
            }
        } catch (Exception e) {
            failures.add("Unable to fetch cluster state: " + e.getMessage());
        }
        return failures;
    }

    public RollingRestartJob abort() {
        final RollingRestartJob job = jobService.findActive()
                .orElseThrow(() -> new IllegalStateException("No active rolling restart job to abort"));
        if (job.smState().isTerminal()) {
            throw new IllegalStateException("Job already in terminal state " + job.smState());
        }
        LOG.info("Abort requested for rolling restart job {}", job.id());
        return jobService.save(job.withAbortRequested(true));
    }

    public RollingRestartJob resume() {
        final RollingRestartJob job = jobService.findActive()
                .orElseThrow(() -> new IllegalStateException("No active rolling restart job to resume"));
        if (job.status() != RollingRestartJobStatus.PAUSED) {
            throw new IllegalStateException("Job is not paused (status=" + job.status() + ")");
        }
        LOG.info("Resuming rolling restart job {} from {}", job.id(), job.smState());
        final StateMachine<RollingRestartState, RollingRestartTrigger> sm =
                RollingRestartStateMachineBuilder.buildFromState(job.smState());
        sm.fire(RollingRestartTrigger.RESUME);
        return jobService.save(job
                .withSmState(sm.getState())
                .withJobStatus(RollingRestartJobStatus.ACTIVE)
                .withPausedReason(null));
    }

    public Optional<RollingRestartJob> currentJob() {
        final Optional<RollingRestartJob> active = jobService.findActive();
        return active.isPresent() ? active : jobService.findLastCompleted();
    }

    public List<RollingRestartJob> history(int limit) {
        return jobService.findHistory(limit);
    }

    public void tick() {
        final Optional<RollingRestartJob> jobOpt = jobService.findActive();
        if (jobOpt.isEmpty()) {
            return;
        }
        RollingRestartJob job = jobOpt.get();
        if (job.status() == RollingRestartJobStatus.PAUSED) {
            return;
        }
        try {
            final RollingRestartJob advanced = advance(job);
            final RollingRestartJob finalJob = advanced.smState().isTerminal()
                    ? advanced.withJobStatus(toJobStatus(advanced.smState())).withFinishedAt(Instant.now())
                    : advanced;
            jobService.save(finalJob);
        } catch (Exception e) {
            LOG.error("Rolling restart tick failed for job {}", job.id(), e);
            try {
                actions.enableAllocation();
            } catch (Exception cleanupError) {
                LOG.warn("Failed to re-enable allocation during failure cleanup", cleanupError);
            }
            jobService.save(job
                    .withLastError(e.getMessage())
                    .withSmState(RollingRestartState.FAILED)
                    .withJobStatus(RollingRestartJobStatus.FAILED)
                    .withFinishedAt(Instant.now()));
        }
    }

    private RollingRestartJob advance(RollingRestartJob job) {
        final StateMachine<RollingRestartState, RollingRestartTrigger> sm =
                RollingRestartStateMachineBuilder.buildFromState(job.smState());

        switch (job.smState()) {
            case PREPARING_CLUSTER -> {
                actions.prepareCluster();
                sm.fire(RollingRestartTrigger.PROCEED);
                return job.withSmState(sm.getState());
            }
            case SELECTING_NEXT_NODE -> {
                if (job.abortRequested()) {
                    sm.fire(RollingRestartTrigger.ABORT);
                    return job.withSmState(sm.getState());
                }
                final int nextIdx = pickNextNodeIndex(job);
                if (nextIdx < 0) {
                    sm.fire(RollingRestartTrigger.NO_MORE_NODES);
                    return job.withSmState(sm.getState());
                }
                sm.fire(RollingRestartTrigger.MORE_NODES);
                final RollingRestartNodeEntry entry = job.nodes().get(nextIdx)
                        .withStatus(RollingRestartNodeStatus.STOPPING)
                        .withStarted(Instant.now());
                return job.withSmState(sm.getState())
                        .withCurrentNodeIndex(nextIdx)
                        .withCurrentNode(entry);
            }
            case STOPPING_NODE -> {
                final RollingRestartNodeEntry current = requireCurrent(job);
                actions.stopNode(current.hostname());
                sm.fire(RollingRestartTrigger.PROCEED);
                return job.withSmState(sm.getState());
            }
            case WAITING_NODE_LEFT -> {
                final RollingRestartNodeEntry current = requireCurrent(job);
                if (!actions.isNodeInCluster(current.hostname())) {
                    sm.fire(RollingRestartTrigger.NODE_LEFT);
                    return job.withSmState(sm.getState())
                            .withCurrentNode(current.withStatus(RollingRestartNodeStatus.STOPPED));
                }
                return job;
            }
            case STARTING_NODE -> {
                final RollingRestartNodeEntry current = requireCurrent(job);
                actions.startNode(current.hostname());
                sm.fire(RollingRestartTrigger.PROCEED);
                return job.withSmState(sm.getState())
                        .withCurrentNode(current.withStatus(RollingRestartNodeStatus.STARTING));
            }
            case WAITING_NODE_JOINED -> {
                final RollingRestartNodeEntry current = requireCurrent(job);
                if (actions.isNodeInCluster(current.hostname())) {
                    sm.fire(RollingRestartTrigger.NODE_JOINED);
                    return job.withSmState(sm.getState())
                            .withCurrentNode(current.withStatus(RollingRestartNodeStatus.STARTED));
                }
                return job;
            }
            case REENABLING_ALLOCATION -> {
                actions.enableAllocation();
                sm.fire(RollingRestartTrigger.PROCEED);
                return job.withSmState(sm.getState());
            }
            case WAITING_GREEN -> {
                if (actions.isClusterGreen()) {
                    sm.fire(RollingRestartTrigger.CLUSTER_GREEN);
                    final RollingRestartNodeEntry current = requireCurrent(job);
                    return job.withSmState(sm.getState())
                            .withCurrentNode(current
                                    .withStatus(RollingRestartNodeStatus.COMPLETED)
                                    .withFinished(Instant.now()));
                }
                final Instant lastTransition = job.updatedAt();
                if (lastTransition != null
                        && Duration.between(lastTransition, Instant.now()).compareTo(GREEN_WAIT_TIMEOUT) > 0) {
                    sm.fire(RollingRestartTrigger.GREEN_TIMEOUT);
                    return job.withSmState(sm.getState())
                            .withJobStatus(RollingRestartJobStatus.PAUSED)
                            .withPausedReason("Cluster did not return to GREEN within "
                                    + GREEN_WAIT_TIMEOUT
                                    + ". Investigate and POST /datanodes/restart/resume to retry.");
                }
                return job;
            }
            case PAUSED_WAITING_GREEN -> {
                return job;
            }
            case FINALIZING -> {
                if (!actions.isAllocationEnabled()) {
                    actions.enableAllocation();
                }
                sm.fire(RollingRestartTrigger.PROCEED);
                return job.withSmState(sm.getState());
            }
            default -> {
                return job;
            }
        }
    }

    private RollingRestartNodeEntry requireCurrent(RollingRestartJob job) {
        final RollingRestartNodeEntry current = job.currentNode();
        if (current == null) {
            throw new IllegalStateException("No current node selected for state " + job.smState());
        }
        return current;
    }

    private int pickNextNodeIndex(RollingRestartJob job) {
        final List<RollingRestartNodeEntry> nodes = job.nodes();
        final Optional<String> manager = actions.electedManagerHostname();

        for (int i = 0; i < nodes.size(); i++) {
            final RollingRestartNodeEntry e = nodes.get(i);
            if (e.status() == RollingRestartNodeStatus.PENDING
                    && (manager.isEmpty() || !manager.get().equals(e.hostname()))) {
                return i;
            }
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).status() == RollingRestartNodeStatus.PENDING) {
                return i;
            }
        }
        return -1;
    }

    private RollingRestartJobStatus toJobStatus(RollingRestartState s) {
        return switch (s) {
            case COMPLETED -> RollingRestartJobStatus.COMPLETED;
            case ABORTED -> RollingRestartJobStatus.ABORTED;
            case FAILED -> RollingRestartJobStatus.FAILED;
            default -> RollingRestartJobStatus.ACTIVE;
        };
    }
}
