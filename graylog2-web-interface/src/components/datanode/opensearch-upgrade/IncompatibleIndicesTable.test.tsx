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
import type { IndexArchiveBinding } from 'components/indices/archive/types';
import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import UserNotification from 'util/UserNotification';

import IncompatibleIndicesTable from './IncompatibleIndicesTable';
import { PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY } from './hooks/usePendingIncompatibleIndexActions';
import useClusterJobs from './hooks/useClusterJobs';
import type { SystemJobSummary } from './hooks/useClusterJobs';
import useArchivedIndexNames from './hooks/useArchivedIndexNames';

jest.mock('components/indices/archive/useIndexArchive');
jest.mock('components/indices/hooks/useIncompatibleIndices');
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

const makeIndex = (overrides: Partial<IncompatibleIndex>): IncompatibleIndex => ({
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

const mockIncompatibleIndices = (overrides: Partial<ReturnType<typeof useIncompatibleIndices>>) => {
  asMock(useIncompatibleIndices).mockReturnValue({
    data: [],
    isError: false,
    isLoading: false,
    refetch: jest.fn(() => Promise.resolve({ data: [] })),
    ...overrides,
  } as ReturnType<typeof useIncompatibleIndices>);
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

const archiveBinding = (): IndexArchiveBinding => ({
  useCanArchive: () => true,
  useArchivedIndexNames: () => new Set<string>(),
  archiveAndDeleteIndex: jest.fn(() => Promise.resolve({ systemJobId: 'archive-job-id' })),
  isArchiveJobConflict: (errorMessage) => errorMessage.includes('already running'),
  archiveSystemJobName: 'org.graylog.plugins.archive.job.ArchiveCreateSystemJob',
});

const storePendingArchive = (indexName: string, systemJobId?: string) => {
  window.localStorage.setItem(
    PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY,
    JSON.stringify([{ action: 'archive-delete', indexName, systemJobId, startedAt: ACTION_STARTED_AT }]),
  );
};

describe('IncompatibleIndicesTable', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.localStorage.removeItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY);
    asMock(useCanArchive).mockReturnValue(true);
    asMock(useIndexArchive).mockReturnValue(archiveBinding());
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: 0 });
    asMock(useArchivedIndexNames).mockReturnValue(new Set());
    mockIncompatibleIndices({});
  });

  it('shows a spinner while loading', async () => {
    mockIncompatibleIndices({ isLoading: true });
    render(<IncompatibleIndicesTable />);

    expect(await screen.findByText(/loading incompatible indices/i)).toBeInTheDocument();
  });

  it('shows an error alert when loading fails', () => {
    mockIncompatibleIndices({ isError: true });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/could not load incompatible indices/i)).toBeInTheDocument();
  });

  it('retries loading incompatible indices on demand from the error state', async () => {
    const refetch = jest.fn(() => Promise.resolve({ data: [] })) as unknown as ReturnType<
      typeof useIncompatibleIndices
    >['refetch'];
    mockIncompatibleIndices({ isError: true, refetch });
    render(<IncompatibleIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /retry now/i }));

    expect(refetch).toHaveBeenCalled();
  });

  it('shows a success message when there are no incompatible indices', () => {
    mockIncompatibleIndices({ data: [] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/no incompatible indices found/i)).toBeInTheDocument();
  });

  it('renders the group counts and the default group rows', () => {
    mockIncompatibleIndices({ data: [graylogIndex, systemIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText('Graylog (1)')).toBeInTheDocument();
    expect(screen.getByText('System (1)')).toBeInTheDocument();
    expect(screen.getByText('Foreign (0)')).toBeInTheDocument();
    expect(screen.getByText('graylog_0')).toBeInTheDocument();
    expect(
      within(screen.getByRole('columnheader', { name: 'Actions' })).getByRole('button', { name: /^delete all/i }),
    ).toBeInTheDocument();
  });

  it('offers archive-and-delete for managed indices only when archiving is available', () => {
    mockIncompatibleIndices({ data: [graylogIndex] });
    const { rerender } = render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive and delete all/i })).not.toBeInTheDocument();

    asMock(useCanArchive).mockReturnValue(false);
    rerender(<IncompatibleIndicesTable />);

    expect(screen.queryByRole('button', { name: /archive and delete/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('only offers reindex for system indices', () => {
    mockIncompatibleIndices({ data: [systemIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /^reindex$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reindex all/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
  });

  it('only offers delete for foreign indices', () => {
    mockIncompatibleIndices({ data: [foreignIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive/i })).not.toBeInTheDocument();
  });

  it('only offers rotate for the active write index', () => {
    mockIncompatibleIndices({ data: [writeIndex] });
    render(<IncompatibleIndicesTable />);

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
    ) as unknown as ReturnType<typeof useIncompatibleIndices>['refetch'];
    mockIncompatibleIndices({ data: [writeIndex], refetch });
    render(<IncompatibleIndicesTable />);

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
    mockIncompatibleIndices({ data: [graylogIndex, writeIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /delete all \(1\)/i })).toBeInTheDocument();
  });

  it('uses the outdated delete endpoint for foreign indices', async () => {
    const sendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    mockIncompatibleIndices({ data: [foreignIndex] });
    render(<IncompatibleIndicesTable />);

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
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

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
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

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
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/archiving\.\.\./i)).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('shows a failed state and keeps row actions when the archive job errored', () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({
      jobsById: new Map([['job-1', clusterJob({ job_status: 'error', info: 'Backend unreachable' })]]),
      jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION,
    });
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/archive failed/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('shows an archived-already badge when the job finished but the index is still incompatible', async () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION });
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    expect(screen.getByText(/archived already/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^archive and delete$/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /delete all \(1\)/i })).toBeInTheDocument();
    await waitFor(() =>
      expect(JSON.parse(window.localStorage.getItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY))).toEqual([
        expect.objectContaining({ action: 'archive-delete', indexName: 'graylog_0', state: 'archived' }),
      ]),
    );
  });

  it('flags an index the archive catalog already knows about, even without a local pending action', () => {
    asMock(useArchivedIndexNames).mockReturnValue(new Set(['graylog_0']));
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/archived already/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^archive and delete$/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
  });

  it('still counts an already-archived index as a bulk delete candidate', () => {
    asMock(useArchivedIndexNames).mockReturnValue(new Set(['graylog_0']));
    mockIncompatibleIndices({ data: [graylogIndex, secondGraylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /delete all \(2\)/i })).toBeInTheDocument();
  });

  it('keeps tracking when the jobs list predates the action (stale cache is not "job gone")', () => {
    storePendingArchive('graylog_0', 'job-1');
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: JOBS_POLLED_BEFORE_ACTION });
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/archiving\.\.\./i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /archive and delete/i })).not.toBeInTheDocument();
    expect(window.localStorage.getItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY)).not.toBe('[]');
  });

  it('drops pending actions for indices that are no longer incompatible', async () => {
    storePendingArchive('graylog_0', 'job-1');
    mockIncompatibleIndices({ data: [foreignIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    await waitFor(() => expect(window.localStorage.getItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY)).toBe('[]'));
  });

  it('keeps tracking an action without a system job id until its index disappears', () => {
    storePendingArchive('graylog_0');
    asMock(useClusterJobs).mockReturnValue({ jobsById: new Map(), jobsUpdatedAt: JOBS_POLLED_AFTER_ACTION });
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByText(/archiving\.\.\./i)).toBeInTheDocument();
    expect(window.localStorage.getItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY)).not.toBe('[]');
  });

  it('ignores malformed localStorage without crashing', () => {
    window.localStorage.setItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY, JSON.stringify({ not: 'an array' }));
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('drops invalid stored entries', () => {
    window.localStorage.setItem(
      PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY,
      JSON.stringify([{ action: 'archive-delete' }, null, 'nonsense']),
    );
    mockIncompatibleIndices({ data: [graylogIndex] });
    render(<IncompatibleIndicesTable />);

    expect(screen.getByRole('button', { name: /^archive and delete$/i })).toBeInTheDocument();
    expect(screen.queryByText(/archiving\.\.\./i)).not.toBeInTheDocument();
  });

  it('runs bulk delete for eligible group indices and reports partial failures', async () => {
    const refetch = jest.fn(() =>
      Promise.resolve({ data: [secondGraylogIndex] }),
    ) as unknown as ReturnType<typeof useIncompatibleIndices>['refetch'];
    asMock(IndexerIndices.remove).mockImplementation((indexName: string) =>
      indexName === 'graylog_1' ? Promise.reject(new Error('Delete failed')) : Promise.resolve(),
    );
    mockIncompatibleIndices({ data: [graylogIndex, secondGraylogIndex], refetch });
    render(<IncompatibleIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete all/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/this will delete 2 incompatible indices/i)).toBeInTheDocument();

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
    const binding = archiveBinding();
    asMock(useIndexArchive).mockReturnValue(binding);
    mockIncompatibleIndices({ data: [graylogIndex, secondGraylogIndex] });
    render(<IncompatibleIndicesTable />);

    await userEvent.click(screen.getAllByRole('button', { name: /^archive and delete$/i })[0]);

    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: /^archive and delete$/i }));

    await waitFor(() => expect(binding.archiveAndDeleteIndex).toHaveBeenCalledWith('graylog_0'));

    const storedActions = JSON.parse(window.localStorage.getItem(PENDING_INCOMPATIBLE_INDEX_ACTIONS_STORAGE_KEY));

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
    mockIncompatibleIndices({ data: [graylogIndex, secondGraylogIndex] });
    render(<IncompatibleIndicesTable />);

    await userEvent.click(screen.getByRole('button', { name: /^delete all/i }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/this will delete 1 incompatible index/i)).toBeInTheDocument();

    await userEvent.click(within(dialog).getByRole('button', { name: /delete all/i }));

    await waitFor(() => expect(IndexerIndices.remove).toHaveBeenCalledTimes(1));
    expect(IndexerIndices.remove).toHaveBeenCalledWith('graylog_1');
    expect(IndexerIndices.remove).not.toHaveBeenCalledWith('graylog_0');
  });
});
