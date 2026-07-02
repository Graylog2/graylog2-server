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
import userEvent from '@testing-library/user-event';

import type { InputTypeDescriptionsResponse } from 'hooks/useInputTypesDescriptions';
import useInputTypesDescriptions from 'hooks/useInputTypesDescriptions';
import useInputMutations from 'hooks/useInputMutations';
import usePermissions from 'hooks/usePermissions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { asMock } from 'helpers/mocking';

import EditInputButton from './EditInputButton';

jest.mock('hooks/useInputTypesDescriptions');
jest.mock('hooks/useInputMutations');
jest.mock('hooks/usePermissions');
jest.mock('logic/telemetry/useSendTelemetry', () => ({
  __esModule: true,
  default: jest.fn(),
}));
jest.mock('routing/useLocation', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('components/inputs', () => ({
  __esModule: true,
  InputForm: ({ handleSubmit, submitButtonText, title }: any) => (
    <div>
      <div>{title}</div>
      <button type="button" onClick={() => handleSubmit({ title: 'Input 3' })}>
        {submitButtonText}
      </button>
    </div>
  ),
  default: {},
}));

const GELF_UDP = 'org.graylog2.inputs.gelf.udp.GELFUDPInput';

const baseInput = {
  id: 'input3',
  title: 'Input 3',
  name: 'Input 3',
  type: GELF_UDP,
  global: false,
  node: 'node2',
  attributes: { foo: 'bar' },
};

const inputTypeDescriptions = {
  [GELF_UDP]: {
    name: 'GELF UDP',
    description: 'GELF UDP Input',
    type: GELF_UDP,
    is_exclusive: false,
    link_to_docs: '',
    requested_configuration: {},
  },
} as unknown as InputTypeDescriptionsResponse;

const renderSUT = (input = baseInput) => render(<EditInputButton input={input as any} />);

describe('EditInputButton', () => {
  const updateInputMock = jest.fn(() => Promise.resolve());

  beforeEach(() => {
    asMock(useInputTypesDescriptions).mockReturnValue({
      data: inputTypeDescriptions,
      isLoading: false,
      refetch: jest.fn(),
    });
    asMock(useInputMutations).mockReturnValue({
      updateInput: updateInputMock,
    } as any);
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useLocation).mockReturnValue({
      pathname: '/system/input/diagnosis/input3',
      search: '',
      hash: '',
      state: null,
      key: 'mock-key',
    } as any);
  });

  it('renders the Edit input button when permitted and the input type is known', () => {
    renderSUT();

    expect(screen.getByRole('button', { name: /edit input/i })).toBeEnabled();
  });

  it('opens the edit form and updates the input on submit', async () => {
    renderSUT();

    await userEvent.click(screen.getByRole('button', { name: /edit input/i }));

    expect(screen.getByText('Editing Input Input 3')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Update input' }));

    expect(updateInputMock).toHaveBeenCalledWith({
      input: expect.objectContaining({ title: 'Input 3' }),
      inputId: 'input3',
    });
  });

  it('disables the button when the input type definition is unavailable', () => {
    asMock(useInputTypesDescriptions).mockReturnValue({
      data: {} as InputTypeDescriptionsResponse,
      isLoading: false,
      refetch: jest.fn(),
    });

    renderSUT();

    expect(screen.getByRole('button', { name: /edit input/i })).toBeDisabled();
  });

  it('does not render the button without edit permissions', () => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => false, isAnyPermitted: () => true });

    renderSUT();

    expect(screen.queryByRole('button', { name: /edit input/i })).not.toBeInTheDocument();
  });
});
