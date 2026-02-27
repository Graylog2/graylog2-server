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
import { render, screen, act } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';
import mockComponent from 'helpers/mocking/MockComponent';
import useInputsStates from 'hooks/useInputsStates';
import { mockInputStates } from 'fixtures/inputs';
import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { asMock } from 'helpers/mocking';
import useFeature from 'hooks/useFeature';
import useInputMutations from 'hooks/useInputMutations';
import usePermissions from 'hooks/usePermissions';

import InputsActions from './InputsActions';

jest.useFakeTimers();

jest.mock('hooks/useInputsStates', () => ({
  __esModule: true,
  default: jest.fn(),
}));
jest.mock('logic/telemetry/useSendTelemetry', () => ({
  __esModule: true,
  default: jest.fn(),
}));
jest.mock('routing/useLocation', () => ({
  __esModule: true,
  default: jest.fn(),
}));
jest.mock('hooks/useFeature');
jest.mock('hooks/useInputMutations');
jest.mock('hooks/usePermissions');
jest.mock('stores/inputs/InputStatesStore', () => ({
  __esModule: true,
  default: {
    setup: jest.fn(),
    stop: jest.fn(),
  },
}));

jest.mock('components/inputs', () => ({
  __esModule: true,
  InputForm: ({ handleSubmit, submitButtonText }: any) => (
    <div>
      <div>Editing Input Input 3</div>
      <button onClick={() => handleSubmit({ title: 'Input 3' })}>{submitButtonText}</button>
    </div>
  ),
  StaticFieldForm: ({ setShowModal }: any) => (
    <div>
      <div>StaticFieldForm</div>
      <button onClick={() => setShowModal(false)}>Close</button>
    </div>
  ),
  InputStateControl: ({ openWizard }: any) => <button onClick={openWizard}>Set up input</button>,
  CreateInputControl: mockComponent('CreateInputControl'),
  InputsList: mockComponent('InputsList'),
  default: {},
}));

jest.mock('components/inputs/InputSetupWizard', () => ({
  __esModule: true,
  InputSetupWizard: () => (
    <div>
      <div>InputSetupWizard</div>
    </div>
  ),
  INPUT_SETUP_MODE_FEATURE_FLAG: 'input_setup_mode',
}));

const baseInput = {
  id: undefined,
  title: undefined,
  type: 'type',
  global: true,
  node: undefined,
  attributes: { foo: 'bar' },
};

const inputTypeDescriptions = {
  'org.graylog2.inputs.gelf.udp.GELFUDPInput': {
    name: 'GELF UDP',
    description: 'GELF UDP Input',
    type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
    is_exclusive: false,
    link_to_docs: '',
    requested_configuration: {},
  },
  'org.graylog.plugins.forwarder.input.ForwarderServiceInput': {
    name: 'Forwarder Service',
    description: 'Forwarder Service Input',
    type: 'org.graylog.plugins.forwarder.input.ForwarderServiceInput',
    is_exclusive: false,
    link_to_docs: '',
    requested_configuration: {},
  },
} as unknown as InputTypeDescriptionsResponse;

const renderSUT = (input = baseInput, extraProps = {}) =>
  render(
    <InputsActions
      input={input as any}
      inputTypes={{} as any}
      inputTypeDescriptions={inputTypeDescriptions}
      currentNode={null}
      {...extraProps}
    />,
  );

const openMoreActions = async () => userEvent.click(await screen.findByRole('button', { name: /more/i }));

describe('InputsActions', () => {
  const updateInputMock = jest.fn(() => Promise.resolve());
  const deleteInputMock = jest.fn(() => Promise.resolve());
  const telemetryMock = jest.fn();

  beforeEach(() => {
    asMock(useInputsStates).mockReturnValue({
      data: mockInputStates,
      isLoading: false,
      refetch: jest.fn(),
    });
    asMock(useLocation).mockImplementation(() => ({
      pathname: '/inputs',
      search: '',
      hash: '',
      state: null,
      key: 'mock-key',
    }));
    asMock(useSendTelemetry).mockReturnValue(telemetryMock);
    asMock(useFeature).mockReturnValue(true);
    asMock(useInputMutations).mockReturnValue({
      updateInput: updateInputMock,
      deleteInput: deleteInputMock,
    } as any);
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true });
  });

  it('renders Received messages button with correct query for standard input', () => {
    renderSUT();
    expect(screen.getByText('Received messages')).toBeInTheDocument();
  });

  it('opens wizard via InputStateControl button', async () => {
    const setupInput = {
      ...baseInput,
      id: 'input3',
      title: 'Input 3',
      type: 'org.graylog.plugins.beats.Beats2Input',
      global: false,
      node: 'node2',
    };

    renderSUT(setupInput);

    act(() => {
      jest.advanceTimersByTime(200);
    });

    const setupButton = await screen.findByRole('button', { name: /set up input/i });

    userEvent.click(setupButton);

    expect(await screen.findByText(/InputSetupWizard/i)).toBeInTheDocument();
  });

  it('renders input state controls when user has changestate permission', async () => {
    const input = {
      ...baseInput,
      id: 'input-changestate',
      title: 'Input with changestate only',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
    };
    const isPermitted = jest.fn((permission) => permission === `inputs:changestate:${input.id}`);

    asMock(usePermissions).mockReturnValue({ isPermitted });

    renderSUT(input);

    expect(await screen.findByRole('button', { name: /set up input/i })).toBeInTheDocument();
    expect(isPermitted).toHaveBeenCalledWith(`inputs:changestate:${input.id}`);
  });

  it('does not render input state controls without changestate permission', () => {
    const input = {
      ...baseInput,
      id: 'input-no-changestate',
      title: 'Input without changestate',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
    };

    asMock(usePermissions).mockReturnValue({ isPermitted: () => false });

    renderSUT(input);

    expect(screen.queryByRole('button', { name: /set up input/i })).not.toBeInTheDocument();
  });

  it('opens Static Field form when Add static field is selected', async () => {
    const input = {
      ...baseInput,
      id: 'inputX',
      title: 'Input X',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
    };
    renderSUT(input);
    await openMoreActions();
    userEvent.click(screen.getByText('Add static field'));

    expect(await screen.findByText('StaticFieldForm')).toBeInTheDocument();
  });

  it('uses forwarder query field for ForwarderServiceInput', () => {
    const input = {
      ...baseInput,
      id: 'forwarder-id',
      title: 'Forwarder Input',
      type: 'org.graylog.plugins.forwarder.input.ForwarderServiceInput',
    };
    renderSUT(input);
    const btn = screen.getByText('Received messages');
    expect(btn).toBeInTheDocument();
    userEvent.click(btn);
    expect(telemetryMock).toHaveBeenCalledWith(
      'Inputs Show Received Messages Clicked',
      expect.objectContaining({
        app_action_value: 'show-received-messages',
      }),
    );
  });

  it('opens the Setup Wizard when clicking Set up input button (from InputStateControl)', async () => {
    const setupInput = {
      ...baseInput,
      id: 'input3',
      title: 'Input 3',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      global: false,
      node: 'node2',
    };

    renderSUT(setupInput);
    const setupButton = await screen.findByRole('button', { name: /set up input/i });
    userEvent.click(setupButton);
    expect(await screen.findByText(/InputSetupWizard/i)).toBeInTheDocument();
  });

  it('shows Edit input and opens InputForm on click', async () => {
    const setupInput = {
      ...baseInput,
      id: 'input3',
      title: 'Input 3',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      global: false,
      node: 'node2',
    };

    renderSUT(setupInput);
    await openMoreActions();

    userEvent.click(screen.getByText('Edit input'));

    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(screen.getByText(/Editing Input Input 3/)).toBeInTheDocument();

    await userEvent.click(screen.getByText('Update input'));
    expect(updateInputMock).toHaveBeenCalledWith({
      input: expect.objectContaining({ title: 'Input 3' }),
      inputId: 'input3',
    });
  });

  it('shows Input Diagnosis menu item', async () => {
    const input = {
      ...baseInput,
      id: 'input1',
      title: 'Diag Input',
      node: 'node1',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
    };
    renderSUT(input);
    await openMoreActions();

    expect(screen.getByText('Input Diagnosis')).toBeInTheDocument();
  });

  it('shows Manage extractors (global) link', async () => {
    const input = {
      ...baseInput,
      id: 'glob1',
      title: 'Global Input',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      global: true,
      node: undefined,
    };
    renderSUT(input);
    await openMoreActions();

    expect(screen.getByText('Manage extractors')).toBeInTheDocument();
  });

  it('shows Manage extractors (local) when input is local', async () => {
    const input = {
      ...baseInput,
      id: 'loc1',
      title: 'Local Input',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      global: false,
      node: 'node-1',
    };
    renderSUT(input);
    await openMoreActions();

    expect(screen.getByText('Manage extractors')).toBeInTheDocument();
  });

  it('shows Delete input and is able to delete input on confirmation', async () => {
    const setupInput = {
      ...baseInput,
      id: 'input3',
      title: 'Input 3',
      type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      global: false,
      node: 'node2',
    };

    renderSUT(setupInput);
    await openMoreActions();

    userEvent.click(screen.getByText('Delete input'));

    expect(screen.getByText('Do you really want to delete input Input 3?')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    expect(deleteInputMock).toHaveBeenCalledWith({ inputId: 'input3' });
  });

  describe('Setup mode actions', () => {
    const { isInputInSetupMode, isInputRunning } = jest.requireMock('components/inputs/helpers/inputState');

    it('shows Enter Setup mode when not running and not in setup mode', async () => {
      asMock(isInputInSetupMode).mockReturnValue(false);
      asMock(isInputRunning).mockReturnValue(false);

      const input = {
        ...baseInput,
        id: 'input4',
        title: 'Setup 1',
        node: 'node1',
        type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      };

      renderSUT(input);
      await openMoreActions();

      const enterItem = screen.getByText('Enter Setup mode');
      expect(enterItem).toBeInTheDocument();

      userEvent.click(enterItem);

      const InputStatesStore = jest.requireMock('stores/inputs/InputStatesStore').default;
      expect(InputStatesStore.setup).toHaveBeenCalledWith(input);
    });

    it('shows Exit Setup mode when in setup mode', async () => {
      asMock(isInputInSetupMode).mockReturnValue(true);
      asMock(isInputRunning).mockReturnValue(false);

      const input = {
        ...baseInput,
        id: 'input3',
        title: 'Setup 2',
        node: 'node1',
        type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      };

      renderSUT(input);
      await openMoreActions();

      const exitItem = screen.getByText('Exit Setup mode');
      expect(exitItem).toBeInTheDocument();

      userEvent.click(exitItem);

      const InputStatesStore = jest.requireMock('stores/inputs/InputStatesStore').default;
      expect(InputStatesStore.stop).toHaveBeenCalledWith(input);
    });

    it('does not show Enter Setup mode when input is running', async () => {
      asMock(isInputInSetupMode).mockReturnValue(false);
      asMock(isInputRunning).mockReturnValue(true);

      const input = {
        ...baseInput,
        id: 'input1',
        title: 'Running Input',
        node: 'node1',
        type: 'org.graylog2.inputs.gelf.udp.GELFUDPInput',
      };

      renderSUT(input);
      await openMoreActions();

      expect(screen.queryByText('Enter Setup mode')).not.toBeInTheDocument();
    });
  });
});
