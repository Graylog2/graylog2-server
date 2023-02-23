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
import { render, screen, within, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';

import fetch from 'logic/rest/FetchProvider';
import { Streams } from '@graylog/server-api';
import UserNotification from 'util/UserNotification';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import { indexSets } from 'fixtures/indexSets';
import { asMock } from 'helpers/mocking';
import suppressConsole from 'helpers/suppressConsole';
import ApiRoutes from 'routing/ApiRoutes';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('@graylog/server-api', () => ({
  Streams: {
    assignToIndexSet: jest.fn(() => Promise.resolve()),
  },
}));

describe('StreamsOverview BulkActionsRow', () => {
  const openActionsDropdown = async () => {
    await screen.findByRole('button', {
      name: /bulk actions/i,
    });
  };

  const assignIndexSet = async () => {
    userEvent.click(await screen.findByRole('menuitem', { name: /assign index set/i }));

    await screen.findByRole('heading', {
      name: /assign index set to 2 streams/i,
      hidden: true,
    });

    const indexSetSelect = await screen.findByLabelText('Index Set');
    selectEvent.openMenu(indexSetSelect);
    await selectEvent.select(indexSetSelect, 'Example Index Set');

    const document = screen.getByRole('document', { hidden: true });

    const submitButton = within(document).getByRole('button', {
      name: /assign index set/i,
      hidden: true,
    });

    await waitFor(() => expect(submitButton).toBeEnabled());

    await suppressConsole(async () => {
      await userEvent.click(submitButton);
    });
  };

  const deleteStreams = async () => {
    userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
  };

  beforeEach(() => {
    window.confirm = jest.fn(() => true);
  });

  it('should assign index set', async () => {
    render(<BulkActions selectedStreamIds={['stream-id-1', 'stream-id-2']}
                        setSelectedStreamIds={() => {}}
                        indexSets={indexSets} />);

    await openActionsDropdown();
    await assignIndexSet();

    await waitFor(() => expect(Streams.assignToIndexSet).toHaveBeenCalledWith('index-set-id-2', ['stream-id-1', 'stream-id-2']));

    expect(UserNotification.success).toHaveBeenCalledWith('Index set was assigned to 2 streams successfully.', 'Success');
  });

  it('should handle errors when assigning index set', async () => {
    asMock(Streams.assignToIndexSet).mockImplementation(() => Promise.reject(new Error('Unexpected error!')));

    render(<BulkActions selectedStreamIds={['stream-id-1', 'stream-id-2']}
                        setSelectedStreamIds={() => {}}
                        indexSets={indexSets} />);

    await openActionsDropdown();
    await assignIndexSet();

    await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith('Assigning index set failed with status: Error: Unexpected error!', 'Error'));
  });

  it('should delete selected streams', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({ failures: [] }));
    const setSelectedStreamIds = jest.fn();

    render(<BulkActions selectedStreamIds={['stream-id-1', 'stream-id-2']}
                        setSelectedStreamIds={setSelectedStreamIds}
                        indexSets={indexSets} />);

    await openActionsDropdown();
    await deleteStreams();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 streams?');

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining(ApiRoutes.StreamsApiController.bulk_delete().url),
      { entity_ids: ['stream-id-1', 'stream-id-2'] },
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('2 streams were deleted successfully.', 'Success');
    expect(setSelectedStreamIds).toHaveBeenCalledWith([]);
  });

  it('should display warning and not reset streams which could not be deleted', async () => {
    asMock(fetch).mockReturnValue(Promise.resolve({
      failures: [
        { entity_id: 'stream-id-1', failure_explanation: 'The stream cannot be deleted.' },
      ],
    }));

    const setSelectedStreamIds = jest.fn();

    render(<BulkActions selectedStreamIds={['stream-id-1', 'stream-id-2']}
                        setSelectedStreamIds={setSelectedStreamIds}
                        indexSets={indexSets} />);

    await deleteStreams();

    expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 streams?');

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining(ApiRoutes.StreamsApiController.bulk_delete().url),
      { entity_ids: ['stream-id-1', 'stream-id-2'] },
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected streams could not be deleted.');
    expect(setSelectedStreamIds).toHaveBeenCalledWith(['stream-id-1']);
  });
});
