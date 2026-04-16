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
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import CollectorsSettings from './CollectorsSettings';

import { useCollectorsConfig, useCollectorInputIds, useCollectorsMutations, useCollectorInputDetails, useCollectorInputMutations } from '../hooks';
import type { CollectorsConfig } from '../types';
import { mockCollectorsMutations } from '../testing/mockMutations';

jest.mock('../hooks');
jest.mock('hooks/useInputsStates');
jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');

const mockInput = (port: number) => ({
  id: 'input-1',
  creator_user_id: 'admin',
  node: 'node-1',
  name: 'CollectorIngestHttpInput',
  created_at: '2026-01-01T00:00:00Z',
  global: true,
  attributes: { port, bind_address: '0.0.0.0' },
  title: 'Collector Ingest (HTTP)',
  type: 'org.graylog.collectors.input.CollectorIngestHttpInput',
  content_pack: '',
  static_fields: {},
});
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
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        updateConfig,
        isUpdatingConfig: false,
      }),
    );
    asMock(useCollectorInputIds).mockReturnValue({
      data: [],
      isLoading: false,
    });
    asMock(useInputsStates).mockReturnValue({
      data: {},
      refetch: jest.fn(),
      isLoading: false,
    });
    asMock(useCollectorInputDetails).mockReturnValue({
      collectorInputIds: [],
      readableInputIds: [],
      loadedInputs: [],
      unreadableCount: 0,
      isLoading: false,
    });
    asMock(useCollectorInputMutations).mockReturnValue({
      createCollectorInput: jest.fn(),
      isCreatingCollectorInput: false,
    });
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

    const hostnameInput = await screen.findByLabelText('External hostname');
    const portInput = screen.getByLabelText('External port');

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

    await screen.findByLabelText('External hostname');

    expect(screen.queryByLabelText('Enabled')).not.toBeInTheDocument();
  });

  it('shows port mismatch info when input port differs from config port', async () => {
    asMock(useCollectorInputDetails).mockReturnValue({
      collectorInputIds: ['input-1'],
      readableInputIds: ['input-1'],
      loadedInputs: [mockInput(14402)],
      unreadableCount: 0,
      isLoading: false,
    });

    render(<CollectorsSettings />);

    await screen.findByText(/different port/i);
    expect(screen.getAllByText(/14402/).length).toBeGreaterThanOrEqual(1);
  });

  it('does not show port mismatch info when ports match', async () => {
    asMock(useCollectorInputDetails).mockReturnValue({
      collectorInputIds: ['input-1'],
      readableInputIds: ['input-1'],
      loadedInputs: [mockInput(14401)],
      unreadableCount: 0,
      isLoading: false,
    });

    render(<CollectorsSettings />);

    await screen.findByLabelText('External hostname');
    expect(screen.queryByText(/different port/i)).not.toBeInTheDocument();
  });

  it('does not show port mismatch info while loading', async () => {
    asMock(useCollectorInputDetails).mockReturnValue({
      collectorInputIds: [],
      readableInputIds: [],
      loadedInputs: [],
      unreadableCount: 0,
      isLoading: true,
    });

    render(<CollectorsSettings />);

    await screen.findByLabelText('External hostname');
    expect(screen.queryByText(/different port/i)).not.toBeInTheDocument();
  });
});

describe('CollectorsSettings telemetry', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCollectorsConfig).mockReturnValue({
      data: config,
      isLoading: false,
    });
    asMock(useCollectorsMutations).mockReturnValue(
      mockCollectorsMutations({
        updateConfig: jest.fn().mockResolvedValue(undefined),
        isUpdatingConfig: false,
      }),
    );
    asMock(useCollectorInputIds).mockReturnValue({
      data: ['input-1'],
      isLoading: false,
    });
    asMock(useInputsStates).mockReturnValue({
      data: {
        'input-1': {
          'node-1': {
            state: 'RUNNING',
            id: 'input-1',
            detailed_message: null,
            message_input: {} as never,
          },
        },
      },
      refetch: jest.fn(),
      isLoading: false,
    } as never);
    asMock(useCollectorInputDetails).mockReturnValue({
      collectorInputIds: ['input-1'],
      readableInputIds: ['input-1'],
      loadedInputs: [mockInput(14401)],
      unreadableCount: 0,
      isLoading: false,
    });
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
    const hostnameInput = await screen.findByLabelText('External hostname');

    await user.clear(hostnameInput);
    await user.type(hostnameInput, 'newhost.example.com');

    await user.click(screen.getByRole('button', { name: /Update settings/i }));

    await waitFor(() => {
      expect(sendTelemetry).toHaveBeenCalledWith(
        'Collector Settings Updated',
        expect.objectContaining({
          http_hostname_kind: 'hostname',
          http_port: 14401,
          http_hostname_changed: true,
          http_port_changed: false,
          port_matches_any_input: true,
          input_bind_types: 'all_wildcard',
          has_running_input: true,
          input_count: 1,
        }),
      );
    });
  });
});
