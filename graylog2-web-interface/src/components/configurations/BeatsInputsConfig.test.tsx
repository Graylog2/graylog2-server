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

import useClusterConfig from 'hooks/useClusterConfig';

import BeatsInputsConfig from './BeatsInputsConfig';

let mockStoredConfig: { max_event_size: number } | null = null;
const mockUpdate = jest.fn();

jest.mock('hooks/useClusterConfig', () => ({
  __esModule: true,
  default: jest.fn(() => ({ data: mockStoredConfig, isLoading: false })),
  useUpdateClusterConfig: () => ({ mutateAsync: mockUpdate }),
}));
jest.mock('util/UserNotification', () => ({ success: jest.fn(), error: jest.fn() }));

const CONFIG_TYPE = 'org.graylog.plugins.beats.BeatsInputDefaultsConfig';

describe('BeatsInputsConfig', () => {
  const mockConfig = (config: { max_event_size: number } | null) => {
    mockStoredConfig = config;
  };

  beforeEach(() => {
    mockUpdate.mockReset();
    mockUpdate.mockResolvedValue(undefined);
  });

  it('shows the built-in default when no configuration is stored', async () => {
    mockConfig(null);

    render(<BeatsInputsConfig />);

    await screen.findByText('16.0MiB (16777216 bytes)');
    expect(useClusterConfig).toHaveBeenCalledWith(CONFIG_TYPE);
  });

  it('shows the stored configuration', async () => {
    mockConfig({ max_event_size: 33554432 });

    render(<BeatsInputsConfig />);

    await screen.findByText('32.0MiB (33554432 bytes)');
  });

  it('saves an updated max event size', async () => {
    mockConfig(null);

    render(<BeatsInputsConfig />);

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));

    const input = await screen.findByLabelText(/max. event size/i);
    await userEvent.clear(input);
    await userEvent.type(input, '33554432');
    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    await waitFor(() => expect(mockUpdate).toHaveBeenCalledWith({ max_event_size: 33554432 }));
  });

  it('rejects a non-positive max event size', async () => {
    mockConfig(null);

    render(<BeatsInputsConfig />);

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));

    const input = await screen.findByLabelText(/max. event size/i);
    await userEvent.clear(input);
    await userEvent.type(input, '0');
    await userEvent.click(await screen.findByRole('button', { name: /update configuration/i }));

    await screen.findByText(/must be a whole number between 1 and/i);
    expect(mockUpdate).not.toHaveBeenCalled();
  });
});
