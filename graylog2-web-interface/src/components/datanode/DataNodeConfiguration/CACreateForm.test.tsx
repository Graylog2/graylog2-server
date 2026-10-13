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
import React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import { CA } from '@graylog/server-api';

import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import UserNotification from 'util/UserNotification';

import CACreateForm from './CACreateForm';

jest.mock('@graylog/server-api', () => ({ CA: { createCA: jest.fn() } }));
jest.mock('stores/sessions/SessionStore', () => ({ SessionStore: MockStore(['isLoggedIn', jest.fn()]) }));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('CACreateForm', () => {
  beforeEach(() => {
    asMock(CA.createCA).mockResolvedValue(undefined);
  });

  const submitForm = async () => {
    await userEvent.click(await screen.findByRole('button', { name: /Create CA/i }));
  };

  it('should create CA', async () => {
    render(<CACreateForm />);

    await submitForm();

    await waitFor(() => expect(CA.createCA).toHaveBeenCalledWith({ organization: 'Graylog CA' }));

    expect(UserNotification.success).toHaveBeenCalledWith('CA created successfully');
  });

  it('should show error when CA creation fails', async () => {
    asMock(CA.createCA).mockRejectedValue(new Error('Error'));

    render(
      <DefaultQueryClientProvider>
        <CACreateForm />
      </DefaultQueryClientProvider>,
    );

    await submitForm();

    await waitFor(() => expect(CA.createCA).toHaveBeenCalledWith({ organization: 'Graylog CA' }));

    expect(UserNotification.error).toHaveBeenCalledWith('CA creation failed with error: Error: Error');
  });
});
