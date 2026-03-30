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
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useInputsStates from 'hooks/useInputsStates';

import CollectorsSettings from './CollectorsSettings';

import { useCollectorsConfig, useCollectorInputIds, useCollectorsMutations } from '../hooks';
import type { CollectorsConfig } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks');
jest.mock('hooks/useInputsStates');
jest.mock('hooks/useInputMutations', () => () => ({
  createInput: jest.fn(),
  updateInput: jest.fn(),
  deleteInput: jest.fn(),
}));
jest.mock('components/inputs/InputStateBadge', () => () => <span>Running</span>);

const updateConfig = jest.fn();

const config: CollectorsConfig = {
  ca_cert_id: 'ca-id',
  signing_cert_id: 'signing-id',
  token_signing_key: {
    public_key: 'pub-key',
    private_key: 'priv-key',
    fingerprint: 'fp',
    created_at: '2026-01-01T00:00:00Z',
  },
  otlp_server_cert_id: 'otlp-id',
  http: {
    hostname: 'otlp.example.com',
    port: 14401,
  },
  collector_offline_threshold: 'PT5M',
  collector_default_visibility_threshold: 'P1D',
  collector_expiration_threshold: 'P7D',
};

describe('CollectorsSettings', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useCollectorsConfig).mockReturnValue({
      data: config,
      isLoading: false,
    });
    asMock(useCollectorInputIds).mockReturnValue({
      data: [],
      isLoading: false,
    });
    asMock(useCollectorsMutations).mockReturnValue(mockCollectorsMutations({
      updateConfig,
      isUpdatingConfig: false,
    }));
    asMock(useInputsStates).mockReturnValue({
      data: {},
      refetch: jest.fn(),
      isLoading: false,
    } as unknown as ReturnType<typeof useInputsStates>);
    updateConfig.mockResolvedValue(undefined);
  });

  it('renders the ingest endpoint section', async () => {
    render(<CollectorsSettings />);

    await screen.findByRole('heading', { name: 'Ingest Endpoint' });

    expect(screen.queryByRole('heading', { name: 'gRPC' })).not.toBeInTheDocument();
  });

  it('saves config with create_input false when already configured', async () => {
    const user = userEvent.setup();

    render(<CollectorsSettings />);

    const hostnameInput = await screen.findByLabelText('Hostname');
    const portInput = screen.getByLabelText('Port');

    await user.clear(hostnameInput);
    await user.type(hostnameInput, 'ingest.example.com');
    await user.clear(portInput);
    await user.type(portInput, '14411');
    await user.click(screen.getByRole('button', { name: /Update settings/i }));

    await waitFor(() =>
      expect(updateConfig).toHaveBeenCalledWith({
        http: {
          hostname: 'ingest.example.com',
          port: 14411,
        },
        create_input: false,
        collector_offline_threshold: 'PT5M',
        collector_default_visibility_threshold: 'P1D',
        collector_expiration_threshold: 'P7D',
      }),
    );
  });

  it('does not show enabled checkbox', async () => {
    render(<CollectorsSettings />);

    await screen.findByLabelText('Hostname');

    expect(screen.queryByLabelText('Enabled')).not.toBeInTheDocument();
  });
});
