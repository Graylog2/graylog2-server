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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { render, waitFor, screen } from 'wrappedTestingLibrary';

import selectEvent from 'helpers/selectEvent';
import { alice } from 'fixtures/users';

import SettingsSection from './SettingsSection';

jest.mock('api/entity-share', () => ({
  prepareEntityShare: jest.fn(() => Promise.resolve()),
  updateEntityShare: jest.fn(() => Promise.resolve()),
  loadUserSharesPaginated: jest.fn(() =>
    Promise.resolve({
      list: require('immutable').List(),
      pagination: { page: 1, perPage: 10, query: '', total: 0, count: 0 },
    }),
  ),
}));
jest.mock('hooks/useEntityShareState', () => {
  const mockSetEntityShareState = jest.fn();

  return {
    __esModule: true,
    default: jest.fn(() => ({ data: undefined })),
    useSetEntityShareState: jest.fn(() => mockSetEntityShareState),
    entityShareQueryKey: jest.fn((grn) => ['entity-share', grn ?? 'new']),
  };
});

const exampleUser = alice.toBuilder().sessionTimeoutMs(36000000).timezone('Europe/Berlin').build();

describe('<SettingsSection />', () => {
  it('should use user settings as initial values', async () => {
    const onSubmitStub = jest.fn();
    render(<SettingsSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);

    const submitButton = screen.getByText('Update Settings');

    await userEvent.click(submitButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({
      session_timeout_ms: exampleUser.sessionTimeoutMs,
      timezone: exampleUser.timezone,
    });
  });

  it('should allow session timeout name and timezone change', async () => {
    const onSubmitStub = jest.fn();

    render(<SettingsSection user={exampleUser} onSubmit={(data) => onSubmitStub(data)} />);

    const timeoutAmountInput = await screen.findByPlaceholderText('Timeout amount');
    const submitButton = screen.getByText('Update Settings');

    expect(timeoutAmountInput).toHaveValue(10);

    await screen.findByText('Hours');

    await userEvent.clear(timeoutAmountInput);
    await userEvent.type(timeoutAmountInput, '40');

    await selectEvent.chooseOption('Timeout unit', 'Days');
    await selectEvent.chooseOption('Time Zone', 'Vancouver');
    await userEvent.click(submitButton);

    await waitFor(() => expect(onSubmitStub).toHaveBeenCalledTimes(1));

    expect(onSubmitStub).toHaveBeenCalledWith({
      session_timeout_ms: 3456000000,
      timezone: 'America/Vancouver',
    });
  });
});
