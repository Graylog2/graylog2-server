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
import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import asMock from 'helpers/mocking/AsMock';
import Store from 'logic/local-storage/Store';
import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

import usePendingIncompatibleIndexActions from './usePendingIncompatibleIndexActions';
import useClusterJobs from './useClusterJobs';
import type { ClusterJobsResult } from './useClusterJobs';

jest.mock('logic/local-storage/Store');
jest.mock('components/indices/archive/useIndexArchive');
jest.mock('./useClusterJobs');

type ClusterJob = ClusterJobsResult['jobsById'] extends Map<string, infer Job> ? Job : never;

const renderPendingActions = (incompatibleIndices: Array<IncompatibleIndex> = []) =>
  renderHook(() =>
    usePendingIncompatibleIndexActions({
      incompatibleIndices,
      isLoading: false,
      isError: false,
      refetch: jest.fn().mockResolvedValue(undefined),
      canArchive: false,
    }),
  );

const reindexJob = (indexName: string, jobStatus: ClusterJob['job_status']): ClusterJob => ({
  id: `job-${indexName}`,
  description: 'Reindexes an outdated index to the current major OpenSearch version.',
  name: 'reindex-outdated-index-v1',
  info: `Reindexing outdated index <${indexName}>.`,
  node_id: 'node-1',
  started_at: '2026-01-01T00:00:00.000Z',
  execution_duration: 'PT0.003S',
  percent_complete: 0,
  is_cancelable: false,
  provides_progress: false,
  job_status: jobStatus,
});

describe('usePendingIncompatibleIndexActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useIndexArchive).mockReturnValue(undefined);
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: 0, refetch: jest.fn() });
    asMock(Store.get).mockReturnValue([]);
    asMock(Store.set).mockImplementation(() => undefined);
  });

  it('does not crash when reading pending actions from blocked storage', () => {
    asMock(Store.get).mockImplementation(() => {
      throw new Error('storage blocked');
    });

    const { result } = renderPendingActions();

    expect(result.current.pendingIndexStatuses.size).toBe(0);
  });

  it('does not crash when storing pending actions fails', () => {
    const action = { action: 'reindex', indexName: 'off_page_index', startedAt: '2026-01-01T00:00:00.000Z' };
    asMock(Store.get).mockReturnValue([action]);
    asMock(Store.set).mockImplementation(() => {
      throw new Error('quota exceeded');
    });

    const { result } = renderPendingActions();

    expect(result.current.pendingIndexStatuses.size).toBe(0);
  });

  it('keeps an in-flight action whose index is not in the current view', () => {
    const action = { action: 'reindex', indexName: 'off_page_index', startedAt: '2026-01-01T00:00:00.000Z' };
    asMock(Store.get).mockReturnValue([action]);

    renderPendingActions([]);

    expect(asMock(Store.set).mock.calls.at(-1)?.[1]).toEqual([action]);
    expect(useClusterJobs).toHaveBeenCalledWith(expect.objectContaining({ poll: true }));
  });

  it('discards a persisted action with the removed archived state', async () => {
    asMock(Store.get).mockReturnValue([
      {
        action: 'archive-delete',
        indexName: 'graylog_0',
        startedAt: '2026-01-01T00:00:00.000Z',
        systemJobId: 'job-1',
        state: 'archived',
      },
    ]);

    renderPendingActions();

    await waitFor(() => expect(asMock(Store.set).mock.calls.at(-1)?.[1]).toEqual([]));
    expect(useClusterJobs).toHaveBeenCalledWith(expect.objectContaining({ poll: false }));
  });

  it('clears terminal archive tracking from storage', async () => {
    const action = {
      action: 'archive-delete',
      indexName: 'graylog_0',
      startedAt: '2026-01-01T00:00:00.000Z',
      systemJobId: 'job-1',
    };
    asMock(Store.get).mockReturnValue([action]);
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map(),
      jobsUpdatedAt: Date.parse(action.startedAt) + 1,
      refetch: jest.fn(),
    });

    renderPendingActions();

    await waitFor(() => expect(asMock(Store.set).mock.calls.at(-1)?.[1]).toEqual([]));
  });

  it.each(['complete', 'cancelled'] as const)('clears %s reindex tracking from storage', async (jobStatus) => {
    const action = { action: 'reindex', indexName: 'graylog_0', startedAt: '2026-01-01T00:00:00.000Z' };
    asMock(Store.get).mockReturnValue([action]);
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map([['job-1', reindexJob(action.indexName, jobStatus)]]),
      jobsUpdatedAt: Date.parse(action.startedAt) + 1,
      refetch: jest.fn(),
    });

    renderPendingActions();

    await waitFor(() => expect(asMock(Store.set).mock.calls.at(-1)?.[1]).toEqual([]));
  });
});
