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
import AppConfig from 'util/AppConfig';
import useInputsStates from 'hooks/useInputsStates';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';

import IngestEndpointStrip from './IngestEndpointStrip';

import {
  useCollectorInputIds,
  useCollectorsMutations,
  useCollectorInputDetails,
  useCollectorInputMutations,
  useCollectorPermissions,
} from '../../hooks';
import type { CollectorsConfig } from '../../types';
import { mockCollectorsMutations } from '../../testing/mockMutations';
import { mockCollectorPermissions } from '../../testing/mockPermissions';

jest.mock('../../hooks');
jest.mock('hooks/useInputsStates');
jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/inputs/InputStateBadge', () => () => <span>state badge</span>);

// The GET response before any config was saved: server-derived hostname, default port and thresholds.
const unconfigured: CollectorsConfig = {
  ca_cert_id: null,
  signing_cert_id: null,
  token_signing_key: null,
  otlp_server_cert_id: null,
  http: { hostname: 'graylog.example.com', port: 14401 },
  collector_heartbeat_interval: 'PT30S',
  collector_offline_threshold: 'PT5M',
  collector_default_visibility_threshold: 'P1D',
  collector_expiration_threshold: 'P7D',
};

const configured: CollectorsConfig = { ...unconfigured, ca_cert_id: 'ca-id', signing_cert_id: 'signing-id' };

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

const withInputs = (inputs: Array<ReturnType<typeof mockInput>>) => {
  const ids = inputs.map((i) => i.id);
  asMock(useCollectorInputIds).mockReturnValue({ data: ids, isLoading: false } as ReturnType<typeof useCollectorInputIds>);
  asMock(useCollectorInputDetails).mockReturnValue({
    collectorInputIds: ids,
    readableInputIds: ids,
    loadedInputs: inputs,
    unreadableCount: 0,
    isLoading: false,
  });
};

const withInputStates = (state: 'RUNNING' | 'FAILED') => {
  asMock(useInputsStates).mockReturnValue({
    data: { 'input-1': { 'node-1': { state, message_input: {} as never, detailed_message: '', started_at: '', id: 'input-1' } } },
    isLoading: false,
  } as unknown as ReturnType<typeof useInputsStates>);
};

// Reader + Collectors Manager: may edit the collectors config but holds no input permissions.
const collectorsManager = () =>
  mockCollectorPermissions({ canCreateIngestInput: false, canEditIngestInput: () => false });

describe('IngestEndpointStrip', () => {
  const updateConfig = jest.fn();
  const createCollectorInput = jest.fn();
  const updateCollectorInputPort = jest.fn();
  const onConfirmed = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
    asMock(useCollectorPermissions).mockReturnValue(mockCollectorPermissions());
    asMock(useCollectorsMutations).mockReturnValue(mockCollectorsMutations({ updateConfig }));
    asMock(useCollectorInputMutations).mockReturnValue({
      createCollectorInput,
      isCreatingCollectorInput: false,
      updateCollectorInputPort,
      isUpdatingCollectorInputPort: false,
    });
    updateCollectorInputPort.mockResolvedValue(undefined);
    asMock(useInputsStates).mockReturnValue({ data: undefined, isLoading: false } as unknown as ReturnType<
      typeof useInputsStates
    >);
    withInputs([]);
    updateConfig.mockResolvedValue(configured);
  });

  describe('before the config exists', () => {
    it('asks to confirm the server-derived endpoint and saves it with the default thresholds', async () => {
      render(<IngestEndpointStrip config={unconfigured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/confirm how collectors reach this cluster/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/external hostname/i)).toHaveValue('graylog.example.com');
      expect(screen.getByText(':')).toBeInTheDocument();
      expect(screen.getByLabelText(/external port/i)).toHaveValue(14401);

      await userEvent.click(screen.getByRole('button', { name: /^confirm$/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith({
          http: { hostname: 'graylog.example.com', port: 14401 },
          collector_offline_threshold: 'PT5M',
          collector_default_visibility_threshold: 'P1D',
          collector_expiration_threshold: 'P7D',
          create_input: true,
        });
      });
      expect(onConfirmed).toHaveBeenCalledTimes(1);
    });

    it('does not request an input when one already exists', async () => {
      withInputs([mockInput(14401)]);

      render(<IngestEndpointStrip config={unconfigured} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /^confirm$/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(expect.objectContaining({ create_input: false }));
      });
    });
  });

  describe('once the config exists', () => {
    it('shows the endpoint as reachable when an ingest input is running', () => {
      withInputs([mockInput(14401)]);
      withInputStates('RUNNING');

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/collectors reach this cluster at/i)).toBeInTheDocument();
      expect(screen.getByText('graylog.example.com:14401')).toBeInTheDocument();
      expect(screen.queryByLabelText(/external hostname/i)).not.toBeInTheDocument();
    });

    it('warns when the ingest input exists but is not running', () => {
      withInputs([mockInput(14401)]);
      withInputStates('FAILED');

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/ingest input .* is not running/i)).toBeInTheDocument();
      expect(screen.getByText(/will not be able to send data/i)).toBeInTheDocument();
      expect(screen.getByText('state badge')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /manage input/i })).toBeInTheDocument();
    });

    it('warns when no ingest input exists and lets a permitted user create one', async () => {
      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/no ingest input exists/i)).toBeInTheDocument();

      await userEvent.click(screen.getByRole('button', { name: /create input/i }));

      expect(createCollectorInput).toHaveBeenCalledTimes(1);
    });

    it('tells a user without input permissions to ask an administrator instead of offering the button', () => {
      asMock(useCollectorPermissions).mockReturnValue(collectorsManager());

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/no ingest input exists/i)).toBeInTheDocument();
      expect(screen.getByText(/administrator/i)).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /create input/i })).not.toBeInTheDocument();
    });

    it('lets the user change the endpoint again', async () => {
      withInputs([mockInput(14401)]);
      withInputStates('RUNNING');

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /change/i }));

      expect(screen.getByLabelText(/external hostname/i)).toHaveValue('graylog.example.com');
    });

    it('moves the existing ingest input to the new port when the user may edit inputs', async () => {
      const input = mockInput(14401);
      withInputs([input]);
      withInputStates('FAILED');

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /change/i }));
      const port = screen.getByLabelText(/external port/i);
      await userEvent.clear(port);
      await userEvent.type(port, '14402');
      await userEvent.click(screen.getByRole('button', { name: /^confirm$/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(
          expect.objectContaining({ http: { hostname: 'graylog.example.com', port: 14402 }, create_input: false }),
        );
      });
      expect(updateCollectorInputPort).toHaveBeenCalledWith({ input, port: 14402 });
    });

    it('leaves the input alone when only the hostname changes', async () => {
      withInputs([mockInput(14401)]);
      withInputStates('RUNNING');

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /change/i }));
      const hostname = screen.getByLabelText(/external hostname/i);
      await userEvent.clear(hostname);
      await userEvent.type(hostname, 'lb.example.com');
      await userEvent.click(screen.getByRole('button', { name: /^confirm$/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(
          expect.objectContaining({ http: { hostname: 'lb.example.com', port: 14401 } }),
        );
      });
      expect(updateCollectorInputPort).not.toHaveBeenCalled();
    });

    it('offers no Change when the user cannot edit the existing input', () => {
      asMock(useCollectorPermissions).mockReturnValue(collectorsManager());
      withInputs([mockInput(14401)]);
      withInputStates('FAILED');

      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/ingest input .* is not running/i)).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /change/i })).not.toBeInTheDocument();
    });

    it('still offers Change when no input exists and the user may create one', () => {
      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByRole('button', { name: /change/i })).toBeInTheDocument();
    });
  });

  describe('in Cloud', () => {
    beforeEach(() => {
      jest.spyOn(AppConfig, 'isCloud').mockReturnValue(true);
    });

    afterEach(() => {
      asMock(AppConfig.isCloud).mockRestore();
    });

    it('initializes the server-provisioned endpoint without showing a form', async () => {
      render(<IngestEndpointStrip config={unconfigured} onConfirmed={onConfirmed} />);

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(expect.objectContaining({ create_input: false }));
      });

      expect(screen.queryByLabelText(/external hostname/i)).not.toBeInTheDocument();
      expect(updateConfig).toHaveBeenCalledTimes(1);
      expect(onConfirmed).toHaveBeenCalledTimes(1);
    });

    it('shows the endpoint as reachable once configured without consulting inputs', () => {
      render(<IngestEndpointStrip config={configured} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/collectors reach this cluster at/i)).toBeInTheDocument();
      expect(screen.queryByText(/no ingest input exists/i)).not.toBeInTheDocument();
    });
  });
});
