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
import { useQuery } from '@tanstack/react-query';

import { ClusterJobs } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

import { ARCHIVE_POLL_INTERVAL_MS } from '../constants';

export type SystemJobSummary = Awaited<ReturnType<typeof ClusterJobs.getJob>>;

export type ClusterJobsResult = {
  jobsById: Map<string, SystemJobSummary>;
  jobsUpdatedAt: number;
  refetch?: () => Promise<unknown>;
};

type Options = {
  enabled: boolean;
  poll: boolean;
};

const useClusterJobs = ({ enabled, poll }: Options): ClusterJobsResult => {
  const { data, dataUpdatedAt, refetch } = useQuery({
    queryKey: ['opensearch-upgrade', 'cluster-jobs'],
    queryFn: () =>
      defaultOnError(
        ClusterJobs.list({ requestShouldExtendSession: false }),
        'Loading cluster jobs failed',
        'Could not load cluster jobs',
      ),
    enabled,
    refetchInterval: poll ? ARCHIVE_POLL_INTERVAL_MS : false,
  });

  const jobsById = new Map<string, SystemJobSummary>();
  Object.values(data ?? {}).forEach((nodeJobs) => {
    Object.values(nodeJobs ?? {}).forEach((jobs) => {
      (jobs ?? []).forEach((job) => jobsById.set(job.id, job));
    });
  });

  return { jobsById, jobsUpdatedAt: dataUpdatedAt, refetch };
};

export default useClusterJobs;
