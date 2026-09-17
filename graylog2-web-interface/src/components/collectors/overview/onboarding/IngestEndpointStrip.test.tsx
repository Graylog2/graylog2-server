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

import { asMock, MockStore } from 'helpers/mocking';
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
import { mockCollectorsMutations } from '../../testing/mockMutations';
import { mockCollectorPermissions } from '../../testing/mockPermissions';
import { configuredCollectorsConfig, mockCollectorInput, unconfiguredCollectorsConfig } from '../../testing/fixtures';

jest.mock('../../hooks');
jest.mock('hooks/useInputsStates');
jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('stores/nodes/NodesStore', () => ({
  NodesStore: MockStore(['getInitialState', () => ({ nodes: { 'node-1': { short_node_id: 'node-1', hostname: 'node-1.example.org' } } })]),
}));

const withInputs = (inputs: Array<ReturnType<typeof mockCollectorInput>>) => {
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
    updateConfig.mockResolvedValue(configuredCollectorsConfig);
  });

  describe('before the config exists', () => {
    it('asks to confirm the server-derived endpoint and saves it with the default thresholds', async () => {
      render(<IngestEndpointStrip config={unconfiguredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/confirm how collectors send data to this cluster/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/external hostname/i)).toHaveValue('graylog.example.com');
      expect(screen.getByText(':')).toBeInTheDocument();
      expect(screen.getByLabelText(/external port/i)).toHaveValue(14401);

      await userEvent.click(screen.getByRole('button', { name: /confirm endpoint/i }));

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
      withInputs([mockCollectorInput(14401)]);

      render(<IngestEndpointStrip config={unconfiguredCollectorsConfig} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /confirm endpoint/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(expect.objectContaining({ create_input: false }));
      });
    });
  });

  describe('once the config exists', () => {
    it('shows the endpoint as reachable when an ingest input is running', () => {
      withInputs([mockCollectorInput(14401)]);
      withInputStates('RUNNING');

      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/collectors send data to this cluster at/i)).toBeInTheDocument();
      expect(screen.getByText('graylog.example.com:14401')).toBeInTheDocument();
      expect(screen.queryByLabelText(/external hostname/i)).not.toBeInTheDocument();
    });

    it('warns when the ingest input exists but is not running', async () => {
      withInputs([mockCollectorInput(14401)]);
      withInputStates('FAILED');

      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/ingest input .* is not running/i)).toBeInTheDocument();
      expect(screen.getByText(/will not be able to send data/i)).toBeInTheDocument();
      expect(await screen.findByText('1 Failed')).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /manage input/i })).toBeInTheDocument();
    });

    it('warns when no ingest input exists and lets a permitted user create one at the confirmed address', async () => {
      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/no ingest input exists/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/external hostname/i)).toHaveValue('graylog.example.com');

      await userEvent.click(screen.getByRole('button', { name: /create input/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(
          expect.objectContaining({ http: { hostname: 'graylog.example.com', port: 14401 }, create_input: true }),
        );
      });
      expect(createCollectorInput).not.toHaveBeenCalled();
      expect(onConfirmed).toHaveBeenCalledTimes(1);
    });

    it('creates the missing input on the port entered inline', async () => {
      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      const port = screen.getByLabelText(/external port/i);
      await userEvent.clear(port);
      await userEvent.type(port, '14402');
      await userEvent.click(screen.getByRole('button', { name: /create input/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(
          expect.objectContaining({ http: { hostname: 'graylog.example.com', port: 14402 }, create_input: true }),
        );
      });
    });

    it('lets the user change the endpoint again', async () => {
      withInputs([mockCollectorInput(14401)]);
      withInputStates('RUNNING');

      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /change/i }));

      expect(screen.getByLabelText(/external hostname/i)).toHaveValue('graylog.example.com');
    });

    it('moves the existing ingest input to the new port when the user may edit inputs', async () => {
      const input = mockCollectorInput(14401);
      withInputs([input]);
      withInputStates('FAILED');

      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /change/i }));
      const port = screen.getByLabelText(/external port/i);
      await userEvent.clear(port);
      await userEvent.type(port, '14402');
      await userEvent.click(screen.getByRole('button', { name: /save endpoint/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(
          expect.objectContaining({ http: { hostname: 'graylog.example.com', port: 14402 }, create_input: false }),
        );
      });
      expect(updateCollectorInputPort).toHaveBeenCalledWith({ input, port: 14402 });
    });

    it('leaves the input alone when only the hostname changes', async () => {
      withInputs([mockCollectorInput(14401)]);
      withInputStates('RUNNING');

      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      await userEvent.click(screen.getByRole('button', { name: /change/i }));
      const hostname = screen.getByLabelText(/external hostname/i);
      await userEvent.clear(hostname);
      await userEvent.type(hostname, 'lb.example.com');
      await userEvent.click(screen.getByRole('button', { name: /save endpoint/i }));

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(
          expect.objectContaining({ http: { hostname: 'lb.example.com', port: 14401 } }),
        );
      });
      expect(updateCollectorInputPort).not.toHaveBeenCalled();
    });

    it('offers no Change when the user cannot edit the existing input', () => {
      asMock(useCollectorPermissions).mockReturnValue(collectorsManager());
      withInputs([mockCollectorInput(14401)]);
      withInputStates('FAILED');

      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/ingest input .* is not running/i)).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /change/i })).not.toBeInTheDocument();
    });

    it('offers no separate Change when no input exists, since the address is edited inline', () => {
      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByRole('button', { name: /create input/i })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /change/i })).not.toBeInTheDocument();
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
      render(<IngestEndpointStrip config={unconfiguredCollectorsConfig} onConfirmed={onConfirmed} />);

      await waitFor(() => {
        expect(updateConfig).toHaveBeenCalledWith(expect.objectContaining({ create_input: false }));
      });

      expect(screen.queryByLabelText(/external hostname/i)).not.toBeInTheDocument();
      expect(updateConfig).toHaveBeenCalledTimes(1);
      expect(onConfirmed).toHaveBeenCalledTimes(1);
    });

    it('shows the endpoint as reachable once configured without consulting inputs', () => {
      render(<IngestEndpointStrip config={configuredCollectorsConfig} onConfirmed={onConfirmed} />);

      expect(screen.getByText(/collectors send data to this cluster at/i)).toBeInTheDocument();
      expect(screen.queryByText(/no ingest input exists/i)).not.toBeInTheDocument();
    });
  });
});
