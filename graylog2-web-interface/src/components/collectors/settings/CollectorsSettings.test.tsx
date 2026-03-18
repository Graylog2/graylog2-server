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
import useInput from 'hooks/useInput';
import useInputsStates from 'hooks/useInputsStates';

import CollectorsSettings from './CollectorsSettings';

import { useCollectorsConfig, useCollectorsMutations } from '../hooks';

jest.mock('../hooks');
jest.mock('hooks/useInput');
jest.mock('hooks/useInputsStates');
jest.mock('components/inputs/InputStateBadge', () => () => <span>Running</span>);

const updateConfig = jest.fn();

const config = {
  ca_cert_id: 'ca-id',
  signing_cert_id: 'signing-id',
  token_signing_cert_id: 'token-id',
  otlp_server_cert_id: 'otlp-id',
  http: {
    enabled: true,
    hostname: 'otlp.example.com',
    port: 14401,
    input_id: 'input-1',
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
    } as unknown as ReturnType<typeof useCollectorsConfig>);
    asMock(useCollectorsMutations).mockReturnValue({
      updateConfig,
      isUpdatingConfig: false,
    } as unknown as ReturnType<typeof useCollectorsMutations>);
    asMock(useInput).mockReturnValue({
      data: { id: 'input-1' },
    } as unknown as ReturnType<typeof useInput>);
    asMock(useInputsStates).mockReturnValue({
      data: {},
      refetch: jest.fn(),
      isLoading: false,
    } as unknown as ReturnType<typeof useInputsStates>);
    updateConfig.mockResolvedValue(undefined);
  });

  it('renders only the HTTP ingest endpoint', async () => {
    render(<CollectorsSettings />);

    expect(await screen.findByRole('heading', { name: 'HTTP' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'gRPC' })).not.toBeInTheDocument();
    expect(screen.getByText('HTTP:')).toBeInTheDocument();
    expect(screen.queryByText('gRPC:')).not.toBeInTheDocument();
  });

  it('saves a request payload without grpc settings', async () => {
    const user = userEvent.setup();

    render(<CollectorsSettings />);

    const hostnameInput = await screen.findByLabelText('Hostname');
    const portInput = screen.getByLabelText('Port');

    await user.clear(hostnameInput);
    await user.type(hostnameInput, 'ingest.example.com');
    await user.clear(portInput);
    await user.type(portInput, '14411');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(updateConfig).toHaveBeenCalledWith({
      http: {
        enabled: true,
        hostname: 'ingest.example.com',
        port: 14411,
      },
      collector_offline_threshold: 'PT5M',
      collector_default_visibility_threshold: 'P1D',
      collector_expiration_threshold: 'P7D',
    }));

    expect(updateConfig).not.toHaveBeenCalledWith(expect.objectContaining({ grpc: expect.anything() }));
  });
});
