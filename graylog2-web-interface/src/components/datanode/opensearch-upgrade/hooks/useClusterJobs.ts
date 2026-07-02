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
import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { ClusterJobs } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

import { ARCHIVE_POLL_INTERVAL_MS } from '../constants';

export type SystemJobSummary = Awaited<ReturnType<typeof ClusterJobs.getJob>>;

// `jobsUpdatedAt` (ms, 0 if never fetched) lets callers tell whether the jobs list is fresh relative to a
// given action — so a stale cached list isn't mistaken for "the job is gone". See usePendingOutdatedIndexActions.
export type ClusterJobsResult = { jobsById: Map<string, SystemJobSummary>; jobsUpdatedAt: number };

/**
 * Polls cluster-wide system jobs (archive-and-delete jobs run on the leader node) and returns them keyed
 * by job id. Only polls while `enabled` so it stays idle when nothing is being tracked.
 */
const useClusterJobs = (enabled: boolean): ClusterJobsResult => {
  const { data, dataUpdatedAt } = useQuery({
    queryKey: ['opensearch-upgrade', 'cluster-jobs'],
    queryFn: () => defaultOnError(ClusterJobs.list(), 'Loading cluster jobs failed', 'Could not load cluster jobs'),
    enabled,
    refetchInterval: enabled ? ARCHIVE_POLL_INTERVAL_MS : false,
  });

  const jobsById = useMemo(() => {
    const map = new Map<string, SystemJobSummary>();

    // Shape: { [nodeId]: { [group]: SystemJobSummary[] } }
    Object.values(data ?? {}).forEach((nodeJobs) => {
      Object.values(nodeJobs ?? {}).forEach((jobs) => {
        (jobs ?? []).forEach((job) => map.set(job.id, job));
      });
    });

    return map;
  }, [data]);

  return { jobsById, jobsUpdatedAt: dataUpdatedAt };
};

export default useClusterJobs;
