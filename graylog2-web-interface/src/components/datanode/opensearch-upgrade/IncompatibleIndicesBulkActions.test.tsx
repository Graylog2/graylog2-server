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

import { SystemIndexerIndices } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import UserNotification from 'util/UserNotification';

import IncompatibleIndicesBulkActions from './IncompatibleIndicesBulkActions';
import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';

jest.mock('@graylog/server-api', () => ({
  SystemIndexerIndices: {
    bulkDeleteOutdated: jest.fn(),
  },
}));
jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('util/UserNotification', () => ({ success: jest.fn(), warning: jest.fn(), error: jest.fn() }));

const makeIndex = (indexName: string): IncompatibleIndexRow => ({
  id: indexName,
  index_name: indexName,
  version: '7.10.2',
  warm_index: false,
  managed_index: false,
  system_index: false,
  active_write_index: null,
  begin: null,
  end: null,
});

const indices = [makeIndex('legacy_0'), makeIndex('legacy_1')];

describe('IncompatibleIndicesBulkActions', () => {
  const setSelectedEntities = jest.fn();
  const refetch = jest.fn();

  const renderBulkActions = () =>
    render(
      <IncompatibleIndicesBulkActions
        indices={indices}
        canArchive={false}
        pendingIndexStatuses={new Map()}
        archivedIndexNames={new Set()}
        refetch={refetch}
      />,
    );

  const confirmBulkDelete = async () => {
    await userEvent.click(screen.getByRole('button', { name: /bulk actions/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete all \(2\)/i }));
    await userEvent.click(await screen.findByRole('button', { name: /^delete all$/i }));
  };

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSelectedEntities).mockReturnValue({
      selectedEntities: indices.map(({ id }) => id),
      setSelectedEntities,
      selectEntity: jest.fn(),
      deselectEntity: jest.fn(),
      toggleEntitySelect: jest.fn(),
      isSomeRowsSelected: false,
      isAllRowsSelected: true,
    });
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
});
