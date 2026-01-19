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
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import selectEvent from 'helpers/selectEvent';
import asMock from 'helpers/mocking/AsMock';
import { InputTypesActions } from 'stores/inputs/InputTypesStore';
import { InputsActions } from 'stores/inputs/InputsStore';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useFeature from 'hooks/useFeature';

import useInputTypes from './useInputTypes';
import CreateInputControl from './CreateInputControl';

jest.mock('./useInputTypes');
jest.mock('stores/inputs/InputTypesStore');
jest.mock('stores/inputs/InputsStore');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('routing/useLocation');
jest.mock('hooks/useFeature');

const SUT = () => (
  <DefaultQueryClientProvider>
    <CreateInputControl />
  </DefaultQueryClientProvider>
);

describe('CreateInputControl', () => {
  beforeEach(() => {
    asMock(useInputTypes).mockReturnValue({
      'input-type-1': 'Input Type 1',
      'input-type-2': 'Input Type 2',
    });

    asMock(InputTypesActions.get).mockResolvedValue({
      requested_configuration: [],
      description: 'Test input type',
    });

    asMock(InputsActions.create).mockResolvedValue({ id: 'input-id-1' });

    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useLocation).mockReturnValue({
      pathname: '/system/inputs',
      search: '',
      hash: '',
      state: undefined,
      key: 'mock-location-key',
    });
    asMock(useFeature).mockReturnValue(false);
  });

  it('renders select and button', async () => {
    render(<SUT />);

    expect(await selectEvent.findSelectInput('Select input')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /launch new input/i })).toBeInTheDocument();
  });

  it('enables button when input is selected using selectEvent', async () => {
    render(<SUT />);

    await selectEvent.chooseOption('select input', 'Input Type 1');
    expect(screen.getByRole('button', { name: /launch new input/i })).toBeEnabled();
  });

  it('shows configuration form after selecting input and submitting', async () => {
    render(<SUT />);

    await selectEvent.chooseOption('select input', 'Input Type 1');
    await userEvent.click(screen.getByRole('button', { name: /launch new input/i }));

    await waitFor(() => {
      expect(screen.getByText(/test input type/i)).toBeInTheDocument();
    });
  });

  it('calls createInput when submitting the configuration form', async () => {
    render(<SUT />);

    await selectEvent.chooseOption('select input', 'Input Type 1');
    await userEvent.click(screen.getByRole('button', { name: /launch new input/i }));

    const title = await screen.findByRole('textbox', {
      name: /title/i,
    });
    await userEvent.type(title, 'Test');

    const submitButton = await screen.findByRole('button', { name: /launch input/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(InputsActions.create).toHaveBeenCalled();
    });
  });
});
