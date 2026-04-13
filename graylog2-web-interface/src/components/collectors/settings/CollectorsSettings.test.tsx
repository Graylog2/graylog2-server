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
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import CollectorsSettings from './CollectorsSettings';

import { useCollectorsConfig, useCollectorsMutations } from '../hooks';
import type { CollectorsConfig } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks');
jest.mock('hooks/useInput');
jest.mock('hooks/useInputsStates');
jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
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
    });
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        updateConfig,
        isUpdatingConfig: false,
      }),
    );
    asMock(useInput).mockReturnValue({
      data: { id: 'input-1' },
    } as ReturnType<typeof useInput>);
    asMock(useInputsStates).mockReturnValue({
      data: {},
      refetch: jest.fn(),
      isLoading: false,
    });
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
    await user.click(screen.getByRole('button', { name: /Update settings/i }));

    await waitFor(() =>
      expect(updateConfig).toHaveBeenCalledWith({
        http: {
          enabled: true,
          hostname: 'ingest.example.com',
          port: 14411,
        },
        collector_offline_threshold: 'PT5M',
        collector_default_visibility_threshold: 'P1D',
        collector_expiration_threshold: 'P7D',
      }),
    );

    expect(updateConfig).not.toHaveBeenCalledWith(expect.objectContaining({ grpc: expect.anything() }));
  });
});

describe('CollectorsSettings telemetry', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCollectorsConfig).mockReturnValue({
      data: {
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
          enabled: true,
          hostname: '10.0.0.5',
          port: 5555,
          input_id: 'input-1',
        },
        collector_offline_threshold: 'PT5M',
        collector_default_visibility_threshold: 'P1D',
        collector_expiration_threshold: 'P7D',
      },
      isLoading: false,
    });
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        updateConfig: jest.fn().mockResolvedValue(undefined),
        isUpdatingConfig: false,
      }),
    );
    asMock(useInput).mockReturnValue({
      data: { id: 'input-1', attributes: { bind_address: '0.0.0.0', port: 5555 } },
    } as ReturnType<typeof useInput>);
    asMock(useInputsStates).mockReturnValue({
      data: {
        'input-1': {
          'node-1': {
            state: 'RUNNING',
            id: 'input-1',
            detailed_message: null,
            message_input: {} as any,
          },
        },
      },
      refetch: jest.fn(),
      isLoading: false,
    } as any);
  });

  it('emits SETTINGS.UPDATED on submit with resulting state and diff flags', async () => {
    const user = userEvent.setup();
    const mockUpdateConfig = jest.fn().mockResolvedValue(undefined);
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        updateConfig: mockUpdateConfig,
        isUpdatingConfig: false,
      }),
    );

    render(<CollectorsSettings />);
    const hostnameInput = await screen.findByLabelText('Hostname');

    await user.clear(hostnameInput);
    await user.type(hostnameInput, 'newhost.example.com');

    await user.click(screen.getByRole('button', { name: /Update settings/i }));

    await waitFor(() => {
      expect(sendTelemetry).toHaveBeenCalledWith(
        'Collector Settings Updated',
        expect.objectContaining({
          http_enabled: true,
          http_hostname_kind: 'hostname',
          http_port: 5555,
          http_enabled_changed: false,
          http_hostname_changed: true,
          http_port_changed: false,
          port_matches_input: true,
          input_bind_type: 'wildcard',
          input_state: 'running',
        }),
      );
    });
  });

  it('emits SETTINGS.DIAGNOSTICS_OPENED when the View Diagnostics link is clicked', async () => {
    const user = userEvent.setup();

    render(<CollectorsSettings />);
    await user.click(screen.getByRole('link', { name: /View Diagnostics/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Collector Settings Diagnostics Opened',
      expect.objectContaining({
        input_id: 'input-1',
        http_hostname_kind: 'ip',
        port_matches_input: true,
        input_bind_type: 'wildcard',
        input_state: 'running',
      }),
    );
  });
});
