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
import * as React from 'react';
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { ClusterDeflector, IndexerIndices } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import OutdatedIndicesTable from './OutdatedIndicesTable';
import { PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY } from './hooks/usePendingOutdatedIndexActions';
import useClusterJobs from './hooks/useClusterJobs';
import type { SystemJobSummary } from './hooks/useClusterJobs';
import useArchivedIndexNames from './hooks/useArchivedIndexNames';

jest.mock('components/indices/hooks/useOutdatedIndices');
jest.mock('components/indices/hooks/useCanArchive');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('./hooks/useClusterJobs');
jest.mock('./hooks/useArchivedIndexNames');
jest.mock('logic/rest/FetchProvider', () => {
  class Builder {
    setHeader() {
      return this;
    }

    setHeaders() {
      return this;
    }

    json() {
      return this;
    }

    text() {
      return this;
    }

    raw() {
      return this;
    }

    build() {
      return Promise.resolve(this);
    }
  }

  return {
    __esModule: true,
    default: jest.fn(() => Promise.resolve({ system_job: { id: 'archive-job-id' } })),
    Builder,
  };
});
jest.mock('@graylog/server-api', () => ({
  IndexerIndices: {
    deleteOutdated: jest.fn(() => Promise.resolve()),
    remove: jest.fn(() => Promise.resolve()),
    reindex: jest.fn(() => Promise.resolve()),
  },
  ClusterDeflector: {
    cycleByindexSetId: jest.fn(() => Promise.resolve()),
  },
}));
jest.mock('util/UserNotification', () => ({ success: jest.fn(), warning: jest.fn(), error: jest.fn() }));

const makeIndex = (overrides: Partial<OutdatedIndex>): OutdatedIndex => ({
  index_name: 'index',
  version: '7.10.2',
  warm_index: false,
  managed_index: false,
  system_index: false,
  active_write_index: null,
  ...overrides,
});

const graylogIndex = makeIndex({ index_name: 'graylog_0', managed_index: true });
const secondGraylogIndex = makeIndex({ index_name: 'graylog_1', managed_index: true });
const systemIndex = makeIndex({ index_name: '.system-index', system_index: true });
const foreignIndex = makeIndex({ index_name: 'legacy-index' });
const writeIndex = makeIndex({ index_name: 'graylog_2', managed_index: true, active_write_index: 'index-set-id' });

const mockOutdatedIndices = (overrides: Partial<ReturnType<typeof useOutdatedIndices>>) => {
  asMock(useOutdatedIndices).mockReturnValue({
    data: [],
    isError: false,
    isLoading: false,
    refetch: jest.fn(() => Promise.resolve({ data: [] })),
    ...overrides,
  } as ReturnType<typeof useOutdatedIndices>);
};

const ACTION_STARTED_AT = '2026-07-02T08:00:00.000Z';
const JOBS_POLLED_AFTER_ACTION = Date.parse('2026-07-02T09:00:00.000Z');
const JOBS_POLLED_BEFORE_ACTION = Date.parse('2026-07-02T07:00:00.000Z');

const clusterJob = (overrides: Partial<SystemJobSummary>): SystemJobSummary =>
  ({
    id: 'job-1',
    name: 'archive-job',
    description: 'Archiving index',
    info: '',
    job_status: 'running',
    percent_complete: 0,
    provides_progress: true,
    is_cancelable: true,
    execution_duration: 'PT1S',
    started_at: ACTION_STARTED_AT,
    node_id: 'node-1',
    ...overrides,
  }) as SystemJobSummary;

const storePendingArchive = (indexName: string, systemJobId?: string) => {
  window.localStorage.setItem(
    PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY,
    JSON.stringify([{ action: 'archive-delete', indexName, systemJobId, startedAt: ACTION_STARTED_AT }]),
  );
};

describe('OutdatedIndicesTable', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.removeItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY);
    asMock(useCanArchive).mockReturnValue(true);
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: 0 });
    asMock(useArchivedIndexNames).mockReturnValue(new Set());
    asMock(fetch).mockResolvedValue({ system_job: { id: 'archive-job-id' } });
    mockOutdatedIndices({});
  });

  it('shows a spinner while loading', async () => {
    mockOutdatedIndices({ isLoading: true });
    render(<OutdatedIndicesTable />);

    expect(await screen.findByText(/loading outdated indices/i)).toBeInTheDocument();
  });

  it('shows an error alert when loading fails', () => {
    mockOutdatedIndices({ isError: true });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/could not load outdated indices/i)).toBeInTheDocument();
  });

  it('retries loading outdated indices on demand from the error state', async () => {
    const refetch = jest.fn(() => Promise.resolve({ data: [] })) as unknown as ReturnType<
      typeof useOutdatedIndices
    >['refetch'];
    mockOutdatedIndices({ isError: true, refetch });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /retry now/i }));

    expect(refetch).toHaveBeenCalled();
  });

  it('shows a success message when there are no outdated indices', () => {
    mockOutdatedIndices({ data: [] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/no outdated indices found/i)).toBeInTheDocument();
  });

  it('renders the group counts and the default group rows', () => {
    mockOutdatedIndices({ data: [graylogIndex, systemIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText('Graylog (1)')).toBeInTheDocument();
    expect(screen.getByText('System (1)')).toBeInTheDocument();
    expect(screen.getByText('Foreign (0)')).toBeInTheDocument();
    expect(screen.getByText('graylog_0')).toBeInTheDocument();
    expect(
      within(screen.getByRole('columnheader', { name: 'Actions' })).getByRole('button', { name: /^delete all/i }),
    ).toBeInTheDocument();
  });

  it('offers archive-and-delete for managed indices only when archiving is available', () => {
    mockOutdatedIndices({ data: [graylogIndex] });
    const { rerender } = render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive and delete all/i })).not.toBeInTheDocument();

    asMock(useCanArchive).mockReturnValue(false);
    rerender(<OutdatedIndicesTable />);

    expect(screen.queryByRole('button', { name: /archive and delete/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('only offers reindex for system indices', () => {
    mockOutdatedIndices({ data: [systemIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /^reindex$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reindex all/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });

  it('only offers delete for foreign indices', () => {
    mockOutdatedIndices({ data: [foreignIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive/i })).not.toBeInTheDocument();
  });

  it('only offers rotate for the active write index', () => {
    mockOutdatedIndices({ data: [writeIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText('active write index')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^rotate$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /reindex/i })).not.toBeInTheDocument();
  });

  it('rotates the active write index via its index set and refreshes the list', async () => {
    const sendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    const refetch = jest.fn(() =>
      Promise.resolve({ data: [makeIndex({ ...writeIndex, active_write_index: null })] }),
    ) as unknown as ReturnType<typeof useOutdatedIndices>['refetch'];
    mockOutdatedIndices({ data: [writeIndex], refetch });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^rotate$/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/is the active write index of its index set/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /^rotate$/i }));

    await waitFor(() => expect(ClusterDeflector.cycleByindexSetId).toHaveBeenCalledWith('index-set-id'));
    expect(sendTelemetry).toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.WRITE_INDEX_ROTATE_CONFIRMED,
      expect.objectContaining({ app_section: 'opensearch-upgrade' }),
    );
    expect(UserNotification.success).toHaveBeenCalledWith(expect.stringContaining('graylog_2'));
    expect(refetch).toHaveBeenCalled();
  });

  it('excludes the active write index from bulk delete', () => {
    mockOutdatedIndices({ data: [graylogIndex, writeIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /delete all \(1\)/i })).toBeInTheDocument();
  });

  it('uses the outdated delete endpoint for foreign indices', async () => {
    const sendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    mockOutdatedIndices({ data: [foreignIndex] });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/this will permanently delete/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /^delete$/i }));

    await waitFor(() => expect(IndexerIndices.deleteOutdated).toHaveBeenCalledWith('legacy-index'));
    expect(sendTelemetry).toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.INDEX_DELETE_CONFIRMED,
      expect.objectContaining({ app_section: 'opensearch-upgrade' }),
    );
    expect(UserNotification.success).toHaveBeenCalled();
  });

  it('uses the generic delete endpoint for Graylog-managed indices', async () => {
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /^delete$/i }));

    await waitFor(() => expect(IndexerIndices.remove).toHaveBeenCalledWith('graylog_0'));
  });

  it('shows archive progress from the system job and hides row actions for a pending index', () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map([['job-1', clusterJob({ job_status: 'running', percent_complete: 42 })]]),
      jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION,
    });
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText('42%')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '42');
    expect(screen.queryByRole('button', { name: /archive and delete/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^delete$/i })).not.toBeInTheDocument();
  });

  it('shows an "Archiving..." label without a bar until there is real progress', () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map([['job-1', clusterJob({ job_status: 'running', percent_complete: 0 })]]),
      jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION,
    });
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/archiving\.\.\./i)).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('shows a failed state and keeps row actions when the archive job errored', () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map([['job-1', clusterJob({ job_status: 'error', info: 'Backend unreachable' })]]),
      jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION,
    });
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/archive failed/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('shows an archived-already badge when the job finished but the index is still outdated', async () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION });
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    expect(screen.getByText(/archived already/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^archive and delete$/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /delete all \(1\)/i })).toBeInTheDocument();
    await waitFor(() =>
      expect(JSON.parse(window.localStorage.getItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY))).toEqual([
        expect.objectContaining({ action: 'archive-delete', indexName: 'graylog_0', state: 'archived' }),
      ]),
    );
  });

  it('flags an index the archive catalog already knows about, even without a local pending action', () => {
    asMock(useArchivedIndexNames).mockReturnValue(new Set(['graylog_0']));
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/archived already/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^archive and delete$/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('still counts an already-archived index as a bulk delete candidate', () => {
    asMock(useArchivedIndexNames).mockReturnValue(new Set(['graylog_0']));
    mockOutdatedIndices({ data: [graylogIndex, secondGraylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /delete all \(2\)/i })).toBeInTheDocument();
  });

  it('keeps tracking when the jobs list predates the action (stale cache is not "job gone")', () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: JOBS_POLLED_BEFORE_ACTION });
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/archiving\.\.\./i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive and delete/i })).not.toBeInTheDocument();
    expect(window.localStorage.getItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY)).not.toBe('[]');
  });

  it('drops pending actions for indices that are no longer outdated', async () => {
    storePendingArchive('graylog_0', 'job-1');
    mockOutdatedIndices({ data: [foreignIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    await waitFor(() => expect(window.localStorage.getItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY)).toBe('[]'));
  });

  it('keeps tracking an action without a system job id until its index disappears', () => {
    storePendingArchive('graylog_0');
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION });
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByText(/archiving\.\.\./i)).toBeInTheDocument();
    expect(window.localStorage.getItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY)).not.toBe('[]');
  });

  it('ignores malformed localStorage without crashing', () => {
    window.localStorage.setItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY, JSON.stringify({ not: 'an array' }));
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('drops invalid stored entries', () => {
    window.localStorage.setItem(
      PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY,
      JSON.stringify([{ action: 'archive-delete' }, null, 'nonsense']),
    );
    mockOutdatedIndices({ data: [graylogIndex] });
    render(<OutdatedIndicesTable />);

    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.queryByText(/archiving\.\.\./i)).not.toBeInTheDocument();
  });

  it('runs bulk delete for eligible group indices and reports partial failures', async () => {
    const refetch = jest.fn(() =>
      Promise.resolve({ data: [secondGraylogIndex] }),
    ) as unknown as ReturnType<typeof useOutdatedIndices>['refetch'];
    asMock(IndexerIndices.remove).mockImplementation((indexName: string) =>
      indexName === 'graylog_1' ? Promise.reject(new Error('Delete failed')) : Promise.resolve(),
    );
    mockOutdatedIndices({ data: [graylogIndex, secondGraylogIndex], refetch });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete all/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/this will delete 2 outdated indices/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /delete all/i }));

    await waitFor(() => expect(IndexerIndices.remove).toHaveBeenCalledWith('graylog_0'));
    expect(IndexerIndices.remove).toHaveBeenCalledWith('graylog_1');
    expect(UserNotification.warning).toHaveBeenCalledWith(
      expect.stringContaining('1 succeeded, 1 failed'),
      'Some indices could not be deleted',
    );
    expect(refetch).toHaveBeenCalled();
  });

  it('tracks a successful archive-and-delete job as a pending action', async () => {
    mockOutdatedIndices({ data: [graylogIndex, secondGraylogIndex] });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getAllByRole('button', { name: /^archive and delete$/i })[0]);

    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /^archive and delete$/i }));

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));

    const storedActions = JSON.parse(window.localStorage.getItem(PENDING_OUTDATED_INDEX_ACTIONS_STORAGE_KEY));

    expect(storedActions).toEqual([
      expect.objectContaining({ action: 'archive-delete', indexName: 'graylog_0', systemJobId: 'archive-job-id' }),
    ]);
    expect(UserNotification.success).toHaveBeenCalledWith('Archive and delete job for "graylog_0" was started.');
  });

  it('skips in-progress archive actions when running a bulk action', async () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map([['job-1', clusterJob({ job_status: 'running', percent_complete: 42 })]]),
      jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION,
    });
    mockOutdatedIndices({ data: [graylogIndex, secondGraylogIndex] });
    render(<OutdatedIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete all/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/this will delete 1 outdated index/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /delete all/i }));

    await waitFor(() => expect(IndexerIndices.remove).toHaveBeenCalledTimes(1));
    expect(IndexerIndices.remove).toHaveBeenCalledWith('graylog_1');
    expect(IndexerIndices.remove).not.toHaveBeenCalledWith('graylog_0');
  });
});
