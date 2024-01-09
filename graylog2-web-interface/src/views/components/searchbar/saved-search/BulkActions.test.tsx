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

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { asMock } from 'helpers/mocking';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

import BulkActions from './BulkActions';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');

describe('SavedSearches BulkActions', () => {
  const openActionsDropdown = async () => {
    await userEvent.click(await screen.findByRole('button', {
      name: /bulk actions/i,
    }));
  };

  const deleteSavedSearch = async () => {
    userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
  };

  beforeEach(() => {
    window.confirm = jest.fn(() => true);

    asMock(useSelectedEntities).mockReturnValue({
      selectedEntities: [],
      setSelectedEntities: () => {},
      selectEntity: () => {},
      deselectEntity: () => {},
    });
  });

  it('should delete selected saved searches', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({ failures: [] }));
    const setSelectedEntities = jest.fn();

    asMock(useSelectedEntities).mockReturnValue({
      selectedEntities: ['saved-search-id-1', 'saved-search-id-2'],
      setSelectedEntities,
      selectEntity: () => {},
      deselectEntity: () => {},
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await deleteSavedSearch();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 saved searches?');

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/views/bulk_delete'),
      { entity_ids: ['saved-search-id-1', 'saved-search-id-2'] },
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('2 saved searches were deleted successfully.', 'Success');
    expect(setSelectedEntities).toHaveBeenCalledWith([]);
  });

  it('should display warning and not reset saved searches which could not be deleted', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({
      failures: [
        { entity_id: 'saved-search-id-1', failure_explanation: 'The saved search cannot be deleted.' },
      ],
    }));

    const setSelectedEntities = jest.fn();

    asMock(useSelectedEntities).mockReturnValue({
      selectedEntities: ['saved-search-id-1', 'saved-search-id-2'],
      setSelectedEntities,
      selectEntity: () => {},
      deselectEntity: () => {},
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await deleteSavedSearch();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 saved searches?');

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/views/bulk_delete'),
      { entity_ids: ['saved-search-id-1', 'saved-search-id-2'] },
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected saved searches could not be deleted.');
    expect(setSelectedEntities).toHaveBeenCalledWith(['saved-search-id-1']);
  });
});
