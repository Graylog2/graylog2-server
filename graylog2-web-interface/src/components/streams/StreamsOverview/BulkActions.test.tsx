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

import { Streams } from '@graylog/server-api';
import UserNotification from 'util/UserNotification';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import { indexSets } from 'fixtures/indexSets';
import { asMock } from 'helpers/mocking';
import suppressConsole from 'helpers/suppressConsole';

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('@graylog/server-api', () => ({
  Streams: {
    assignToIndexSet: jest.fn(() => Promise.resolve()),
  },
}));

describe('StreamsOverview BulkActions', () => {
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

  beforeEach(() => {
    window.confirm = jest.fn(() => true);
  });

  it('should assign index set', async () => {
    const refetchStreams = jest.fn();

    render(<BulkActions selectedStreamIds={['stream-id-1', 'stream-id-2']}
                        setSelectedStreamIds={() => {}}
                        indexSets={indexSets}
                        refetchStreams={refetchStreams} />);

    await assignIndexSet();

    await waitFor(() => expect(Streams.assignToIndexSet).toHaveBeenCalledWith('index-set-id-2', ['stream-id-1', 'stream-id-2']));

    expect(UserNotification.success).toHaveBeenCalledWith('Index set was assigned to 2 streams successfully.', 'Success');
    expect(refetchStreams).toHaveBeenCalledTimes(1);
  });

  it('should handle errors when assigning index set', async () => {
    asMock(Streams.assignToIndexSet).mockImplementation(() => Promise.reject(new Error('Unexpected error!')));

    render(<BulkActions selectedStreamIds={['stream-id-1', 'stream-id-2']}
                        setSelectedStreamIds={() => {}}
                        indexSets={indexSets}
                        refetchStreams={() => {}} />);

    await assignIndexSet();

    await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith('Assigning index set failed with status: Error: Unexpected error!', 'Error'));
  });
});
