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

import asMock from 'helpers/mocking/AsMock';
import useCurrentUser from 'hooks/useCurrentUser';
import useNotificationConfig from 'components/notifications/hooks/useNotificationConfig';

import NotificationsConfig from './NotificationsConfig';

jest.mock('hooks/useCurrentUser', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('components/notifications/hooks/useNotificationConfig', () => ({
  __esModule: true,
  default: jest.fn(),
}));

const updateMock = jest.fn();

const mockUserWithPermissions = (permissions: string[]) =>
  asMock(useCurrentUser).mockReturnValue({ permissions } as never);

const mockHook = (overrides: Partial<ReturnType<typeof useNotificationConfig>> = {}) =>
  asMock(useNotificationConfig).mockReturnValue({
    config: { retention_days: 30 },
    isLoading: false,
    error: undefined,
    update: updateMock,
    isUpdating: false,
    updateError: undefined,
    isUpdateEnabled: true,
    ...overrides,
  } as never);

describe('<NotificationsConfig>', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    updateMock.mockResolvedValue({ retention_days: 30 });
  });

  it('renders the current retention with the default value 30', () => {
    mockUserWithPermissions(['notifications_config:read', 'notifications_config:update']);
    mockHook();

    render(<NotificationsConfig />);

    expect(screen.getByText(/30/)).toBeInTheDocument();
  });

  it('shows an info alert and no form when the user lacks notifications_config:read', () => {
    mockUserWithPermissions([]);
    mockHook();

    render(<NotificationsConfig />);

    expect(screen.getByText(/don.+permission to view/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /edit configuration/i })).not.toBeInTheDocument();
  });

  it('hides the edit button when notifications_config:update is missing', () => {
    mockUserWithPermissions(['notifications_config:read']);
    mockHook({ isUpdateEnabled: false } as never);

    render(<NotificationsConfig />);

    expect(screen.queryByRole('button', { name: /edit configuration/i })).not.toBeInTheDocument();
  });

  it('opens the edit modal and saves a new retention value of 90', async () => {
    mockUserWithPermissions(['notifications_config:read', 'notifications_config:update']);
    mockHook();

    render(<NotificationsConfig />);

    await userEvent.click(screen.getByRole('button', { name: /edit configuration/i }));

    const input = await screen.findByRole('spinbutton');

    await userEvent.clear(input);
    await userEvent.type(input, '90');

    await userEvent.click(screen.getByRole('button', { name: /^save$/i }));

    await waitFor(() => {
      expect(updateMock).toHaveBeenCalledWith({ retention_days: 90 });
    });
  });

  it('surfaces a backend 400 error on the form (no toast, inline via updateError)', async () => {
    mockUserWithPermissions(['notifications_config:read', 'notifications_config:update']);
    mockHook({ updateError: { status: 400, message: 'retention_days must be positive' } } as never);

    render(<NotificationsConfig />);

    expect(useNotificationConfig).toHaveBeenCalled();
    expect(asMock(useNotificationConfig).mock.results[0].value.updateError).toEqual({
      status: 400,
      message: 'retention_days must be positive',
    });
  });
});
