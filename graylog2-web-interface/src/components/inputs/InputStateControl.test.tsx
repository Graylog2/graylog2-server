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
import { render, screen } from 'wrappedTestingLibrary';

import useFeature from 'hooks/useFeature';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import type { Input } from 'components/messageloaders/Types';
import type { InputStates } from 'hooks/useInputsStates';
import { asMock } from 'helpers/mocking';

import InputStateControl from './InputStateControl';

jest.mock('logic/telemetry/useSendTelemetry', () => ({
  __esModule: true,
  default: jest.fn(),
}));
jest.mock('routing/useLocation', () => ({
  __esModule: true,
  default: jest.fn(),
}));
jest.mock('hooks/useFeature');

const baseInput: Input = {
  id: 'input-id',
  title: 'Test Input',
  name: 'Test Input',
  type: 'org.graylog2.inputs.raw.udp.RawUDPInput',
  global: true,
  node: 'node-1',
  created_at: '2024-01-01T00:00:00.000Z',
  creator_user_id: 'user',
  content_pack: false,
  static_fields: {},
  attributes: {},
};

const renderSUT = (inputStates: InputStates, featureEnabled = true) => {
  asMock(useFeature).mockReturnValue(featureEnabled);
  asMock(useSendTelemetry).mockReturnValue(jest.fn());
  asMock(useLocation).mockReturnValue({
    pathname: '/system/inputs',
    search: '',
    hash: '',
    state: null,
    key: 'mock-key',
  });

  return render(<InputStateControl input={baseInput} inputStates={inputStates} openWizard={jest.fn()} />);
};

const messageInput = {
  title: baseInput.title,
  global: baseInput.global,
  name: baseInput.name,
  content_pack: '',
  id: baseInput.id,
  created_at: baseInput.created_at,
  type: baseInput.type,
  creator_user_id: baseInput.creator_user_id,
  attributes: baseInput.attributes,
  static_fields: baseInput.static_fields,
  node: baseInput.node,
};

describe('InputStateControl', () => {
  it('shows start when feature is enabled and input has no state', async () => {
    renderSUT({});

    expect(await screen.findByRole('button', { name: /start input/i })).toBeInTheDocument();
  });

  it('shows start when feature is disabled and input has no state', async () => {
    renderSUT({}, false);

    expect(await screen.findByRole('button', { name: /start input/i })).toBeInTheDocument();
  });

  it('shows setup when input is explicitly in SETUP state', async () => {
    const setupStates: InputStates = {
      [baseInput.id]: {
        node1: {
          id: baseInput.id,
          state: 'SETUP',
          detailed_message: null,
          message_input: messageInput,
        },
      },
    };

    renderSUT(setupStates);

    expect(await screen.findByRole('button', { name: /set-up input/i })).toBeInTheDocument();
  });

  it('shows stop when input is running', async () => {
    const runningStates: InputStates = {
      [baseInput.id]: {
        node1: {
          id: baseInput.id,
          state: 'RUNNING',
          detailed_message: null,
          message_input: messageInput,
        },
      },
    };

    renderSUT(runningStates);

    expect(await screen.findByRole('button', { name: /stop input/i })).toBeInTheDocument();
  });

  it('shows start after stopping an input instead of setup', async () => {
    const runningStates: InputStates = {
      [baseInput.id]: {
        node1: {
          id: baseInput.id,
          state: 'RUNNING',
          detailed_message: null,
          message_input: messageInput,
        },
      },
    };

    const { rerender } = renderSUT(runningStates);

    expect(await screen.findByRole('button', { name: /stop input/i })).toBeInTheDocument();

    rerender(<InputStateControl input={baseInput} inputStates={{}} openWizard={jest.fn()} />);

    expect(await screen.findByRole('button', { name: /start input/i })).toBeInTheDocument();
  });
});
