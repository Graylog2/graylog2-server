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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { SystemIndexerIndices, ClusterDeflector } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useIndexArchive from 'components/indices/archive/useIndexArchive';
import type { IndexArchiveBinding } from 'components/indices/archive/types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import UserNotification from 'util/UserNotification';

import IncompatibleIndicesBulkActions from './IncompatibleIndicesBulkActions';
import IncompatibleIndicesContext from './IncompatibleIndicesContext';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';

jest.mock('@graylog/server-api', () => ({
  SystemIndexerIndices: {
    bulkDeleteOutdated: jest.fn(),
    bulkReindex: jest.fn(),
  },
  ClusterDeflector: {
    bulkcycle: jest.fn(),
  },
}));
jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');
jest.mock('components/indices/archive/useIndexArchive');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('util/UserNotification', () => ({ success: jest.fn(), warning: jest.fn(), error: jest.fn() }));

const bulkCycleSuccess = (failures: Array<{ entity_id: string; failure_explanation: string }> = []) => ({
  success: true,
  code: 200,
  error_text: '',
  entity: { successfully_performed: 0, failures, errors: [] },
});

const makeIndex = (indexName: string, overrides: Partial<IncompatibleIndexRow> = {}): IncompatibleIndexRow => ({
  id: indexName,
  index_name: indexName,
  version: '7.10.2',
  warm_index: false,
  managed_index: false,
  system_index: false,
  active_write_index: null,
  begin: null,
  end: null,
  ...overrides,
});

const indices = [makeIndex('legacy_0'), makeIndex('legacy_1')];
const systemIndices = [
  makeIndex('gl-system-events_0', { system_index: true }),
  makeIndex('gl-system-events_1', { system_index: true }),
];
const writeIndices = [
  makeIndex('graylog_42', { active_write_index: 'index-set-a' }),
  makeIndex('events_7', { active_write_index: 'index-set-b' }),
];
const managedIndices = [
  makeIndex('graylog_0', { managed_index: true }),
  makeIndex('graylog_1', { managed_index: true }),
];

const mockSelectedEntities = (selected: Array<string>, setSelectedEntities: jest.Mock) =>
  asMock(useSelectedEntities).mockReturnValue({
    selectedEntities: selected,
    setSelectedEntities,
    selectEntity: jest.fn(),
    deselectEntity: jest.fn(),
    toggleEntitySelect: jest.fn(),
    isSomeRowsSelected: false,
    isAllRowsSelected: true,
  });

describe('IncompatibleIndicesBulkActions', () => {
  const setSelectedEntities = jest.fn();
  const refetch = jest.fn();
  const addArchiveDeleteAction = jest.fn();
  const addReindexAction = jest.fn();
  const refetchClusterJobs = jest.fn();

  const renderBulkActions = (
    rows: Array<IncompatibleIndexRow> = indices,
    canArchive = false,
    pendingIndexStatuses: Map<string, PendingIndexStatus> = new Map(),
  ) =>
    render(
      <IncompatibleIndicesContext.Provider
        value={{
          archiveActionsAvailable: canArchive,
          archivedIndexNames: new Set<string>(),
          pendingIndexStatuses,
          addArchiveDeleteAction,
          addReindexAction,
          refetchClusterJobs,
          refetch,
        }}>
        <IncompatibleIndicesBulkActions indices={rows} />
      </IncompatibleIndicesContext.Provider>,
    );

  const confirmBulkDelete = async () => {
    await userEvent.click(screen.getByRole('button', { name: /bulk actions/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /^delete$/i }));
    await userEvent.click(await screen.findByRole('button', { name: /^delete all$/i }));
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockSelectedEntities(
      indices.map(({ id }) => id),
      setSelectedEntities,
    );
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  it('deletes all selected indices, clears the selection, and refreshes the table', async () => {
    asMock(SystemIndexerIndices.bulkDeleteOutdated).mockResolvedValue({
      successfully_performed: 2,
      failures: [],
      errors: [],
    });
    renderBulkActions();

    await confirmBulkDelete();

    await waitFor(() => {
      expect(SystemIndexerIndices.bulkDeleteOutdated).toHaveBeenCalledWith({
        entity_ids: ['legacy_0', 'legacy_1'],
      });
      expect(UserNotification.success).toHaveBeenCalledWith('2 indices were deleted.');
      expect(setSelectedEntities).toHaveBeenCalledWith([]);
      expect(refetch).toHaveBeenCalledTimes(1);
    });
  });

  it('keeps failed indices selected and warns with the failure details', async () => {
    asMock(SystemIndexerIndices.bulkDeleteOutdated).mockResolvedValue({
      successfully_performed: 1,
      failures: [{ entity_id: 'legacy_1', failure_explanation: 'Delete failed' }],
      errors: [],
    });
    renderBulkActions();

    await confirmBulkDelete();

    await waitFor(() => {
      expect(UserNotification.warning).toHaveBeenCalledWith(
        '1 succeeded, 1 failed.\nlegacy_1: Delete failed',
        'Some indices could not be deleted',
      );
      expect(setSelectedEntities).toHaveBeenCalledWith(['legacy_1']);
      expect(refetch).toHaveBeenCalledTimes(1);
    });
  });

  it('reports request failures without changing the selection or refreshing', async () => {
    asMock(SystemIndexerIndices.bulkDeleteOutdated).mockRejectedValue(new Error('Backend unavailable'));
    renderBulkActions();

    await confirmBulkDelete();

    await waitFor(() =>
      expect(UserNotification.error).toHaveBeenCalledWith('Backend unavailable', 'Could not delete all.'),
    );
    expect(setSelectedEntities).not.toHaveBeenCalled();
    expect(refetch).not.toHaveBeenCalled();
  });

  describe('bulk reindex', () => {
    const confirmBulkReindex = async () => {
      await userEvent.click(screen.getByRole('button', { name: /bulk actions/i }));
      await userEvent.click(await screen.findByRole('menuitem', { name: /reindex system indices/i }));
      await userEvent.click(await screen.findByRole('button', { name: /^reindex all$/i }));
    };

    beforeEach(() => {
      mockSelectedEntities(
        systemIndices.map(({ id }) => id),
        setSelectedEntities,
      );
    });

    it('reindexes all selected system indices via the bulk endpoint, clears the selection, and refreshes', async () => {
      asMock(SystemIndexerIndices.bulkReindex).mockResolvedValue(undefined);
      renderBulkActions(systemIndices);

      await confirmBulkReindex();

      await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));

      expect(SystemIndexerIndices.bulkReindex).toHaveBeenCalledWith({
        indices: ['gl-system-events_0', 'gl-system-events_1'],
        with_replication: true,
      });
      expect(addReindexAction).toHaveBeenCalledWith({ indexName: 'gl-system-events_0' });
      expect(addReindexAction).toHaveBeenCalledWith({ indexName: 'gl-system-events_1' });
      expect(UserNotification.success).toHaveBeenCalledWith('Reindex started for 2 system indices.');
      expect(setSelectedEntities).toHaveBeenCalledWith([]);
    });

    it('reports request failures without changing the selection or refreshing', async () => {
      asMock(SystemIndexerIndices.bulkReindex).mockRejectedValue(new Error('Backend unavailable'));
      renderBulkActions(systemIndices);

      await confirmBulkReindex();

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith('Backend unavailable', 'Could not reindex all.'),
      );
      expect(setSelectedEntities).not.toHaveBeenCalled();
      expect(refetch).not.toHaveBeenCalled();
    });

    it('excludes an index that is already reindexing from the bulk candidates', async () => {
      renderBulkActions(systemIndices, false, new Map([['gl-system-events_0', { state: 'reindexing' }]]));

      await userEvent.click(screen.getByRole('button', { name: /bulk actions/i }));
      await userEvent.click(await screen.findByRole('menuitem', { name: /reindex system indices/i }));

      expect(await screen.findByText(/this will reindex 1 incompatible index\./i)).toBeInTheDocument();
    });
  });

  describe('bulk rotate', () => {
    const confirmBulkRotate = async () => {
      await userEvent.click(screen.getByRole('button', { name: /bulk actions/i }));
      await userEvent.click(await screen.findByRole('menuitem', { name: /rotate active write indices/i }));
      await userEvent.click(await screen.findByRole('button', { name: /^rotate all$/i }));
    };

    beforeEach(() => {
      mockSelectedEntities(
        writeIndices.map(({ id }) => id),
        setSelectedEntities,
      );
    });

    it('rotates the index sets of the selected write indices, clears the selection, and refreshes', async () => {
      asMock(ClusterDeflector.bulkcycle).mockResolvedValue(bulkCycleSuccess());
      renderBulkActions(writeIndices);

      await confirmBulkRotate();

      await waitFor(() => {
        expect(ClusterDeflector.bulkcycle).toHaveBeenCalledWith({ entity_ids: ['index-set-a', 'index-set-b'] });
        expect(UserNotification.success).toHaveBeenCalledWith('2 indices were rotated.');
        expect(setSelectedEntities).toHaveBeenCalledWith([]);
        expect(refetch).toHaveBeenCalledTimes(1);
      });
    });

    it('maps failed index sets back to index names and keeps them selected', async () => {
      asMock(ClusterDeflector.bulkcycle).mockResolvedValue(
        bulkCycleSuccess([{ entity_id: 'index-set-b', failure_explanation: 'Too many aliases' }]),
      );
      renderBulkActions(writeIndices);

      await confirmBulkRotate();

      await waitFor(() => {
        expect(UserNotification.warning).toHaveBeenCalledWith(
          '1 succeeded, 1 failed.\nevents_7: Too many aliases',
          'Some indices could not be rotated',
        );
        expect(setSelectedEntities).toHaveBeenCalledWith(['events_7']);
      });
    });
  });

  describe('bulk archive and delete', () => {
    const setSelectedEntitiesArchive = jest.fn();

    const archiveBinding: jest.Mocked<IndexArchiveBinding> = {
      useCanArchive: jest.fn().mockReturnValue(true),
      useArchivedIndexNames: jest.fn().mockReturnValue(new Set<string>()),
      archiveAndDeleteIndex: jest.fn(),
      archiveAndDeleteIndices: jest.fn(),
      isArchiveJobConflict: jest.fn().mockReturnValue(false),
      archiveSystemJobName: 'archive-job',
    };

    const confirmBulkArchive = async () => {
      await userEvent.click(screen.getByRole('button', { name: /bulk actions/i }));
      await userEvent.click(await screen.findByRole('menuitem', { name: /^archive and delete$/i }));
      await userEvent.click(await screen.findByRole('button', { name: /^archive and delete all$/i }));
    };

    beforeEach(() => {
      mockSelectedEntities(
        managedIndices.map(({ id }) => id),
        setSelectedEntitiesArchive,
      );
      archiveBinding.isArchiveJobConflict.mockReturnValue(false);
      asMock(useIndexArchive).mockReturnValue(archiveBinding);
    });

    it('starts a bulk archive job, tracks each index, clears the selection, and polls jobs', async () => {
      asMock(archiveBinding.archiveAndDeleteIndices).mockResolvedValue({ systemJobId: 'job-1' });
      renderBulkActions(managedIndices, true);

      await confirmBulkArchive();

      await waitFor(() => expect(refetch).toHaveBeenCalledTimes(1));

      expect(archiveBinding.archiveAndDeleteIndices).toHaveBeenCalledWith(['graylog_0', 'graylog_1']);
      expect(addArchiveDeleteAction).toHaveBeenCalledWith({ indexName: 'graylog_0', systemJobId: 'job-1' });
      expect(addArchiveDeleteAction).toHaveBeenCalledWith({ indexName: 'graylog_1', systemJobId: 'job-1' });
      expect(UserNotification.success).toHaveBeenCalledWith('Archive and delete started for 2 indices.');
      expect(setSelectedEntitiesArchive).toHaveBeenCalledWith([]);
      expect(refetchClusterJobs).toHaveBeenCalled();
    });

    it('warns about a running archive job without changing the selection or refreshing', async () => {
      asMock(archiveBinding.isArchiveJobConflict).mockReturnValue(true);
      asMock(archiveBinding.archiveAndDeleteIndices).mockRejectedValue(
        new Error('An archive job is already running!'),
      );
      renderBulkActions(managedIndices, true);

      await confirmBulkArchive();

      await waitFor(() => {
        expect(UserNotification.warning).toHaveBeenCalledWith(
          'Another archive job is already running. New archive jobs can be started after it finishes.',
          'Archive job already running',
        );
      });
      expect(setSelectedEntitiesArchive).not.toHaveBeenCalled();
      expect(refetch).not.toHaveBeenCalled();
    });
  });
});
