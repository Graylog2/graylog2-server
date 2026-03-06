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

import { asMock } from 'helpers/mocking';
import useInputsStates from 'hooks/useInputsStates';
import { useStore } from 'stores/connect';
import { useQueryParams } from 'routing/QueryParams';
import type { InputSummary } from 'hooks/usePaginatedInputs';

import InputsNotifications from './InputsNotifications';

const mockInputsActionsList = jest.fn();
const mockSetQueryParams = jest.fn();

jest.mock('hooks/useInputsStates');
jest.mock('stores/connect', () => ({
  useStore: jest.fn(),
}));
jest.mock('stores/inputs/InputsStore', () => ({
  InputsStore: {},
  InputsActions: {
    list: (...args: any) => mockInputsActionsList(...args),
  },
}));
jest.mock('routing/QueryParams', () => ({
  ...jest.requireActual('routing/QueryParams'),
  useQueryParams: jest.fn(),
}));

const buildInputState = (state: 'RUNNING' | 'FAILED' | 'FAILING' | 'SETUP') =>
  ({ state, id: 'state-id', detailed_message: null, message_input: {} as InputSummary });

describe('<InputsNotifications />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useQueryParams).mockReturnValue([{}, mockSetQueryParams]);
    asMock(useStore).mockReturnValue([{ id: 'input-1' }, { id: 'input-2' }]);
  });

  it('renders filter links for failed/setup/stopped warnings', () => {
    asMock(useInputsStates).mockReturnValue({
      isLoading: false,
      refetch: jest.fn(),
      data: {
        'input-1': {
          node1: buildInputState('FAILED'),
          node2: buildInputState('SETUP'),
        },
      },
    });

    render(<InputsNotifications />);

    expect(screen.getByRole('button', { name: 'Show failed inputs' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show inputs in setup mode' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show stopped inputs' })).toBeInTheDocument();
  });

  it.each([
    ['Show failed inputs', 'FAILED'],
    ['Show inputs in setup mode', 'SETUP'],
    ['Show stopped inputs', 'NOT_RUNNING'],
  ])('applies %s filter and resets table filter state', async (linkText, status) => {
    asMock(useInputsStates).mockReturnValue({
      isLoading: false,
      refetch: jest.fn(),
      data: {
        'input-1': {
          node1: buildInputState('FAILED'),
          node2: buildInputState('SETUP'),
        },
      },
    });

    render(<InputsNotifications />);
    await userEvent.click(screen.getByRole('button', { name: linkText }));

    expect(mockSetQueryParams).toHaveBeenCalledWith({
      filters: [`runtime_status=${status}`],
      page: 1,
      slice: undefined,
      sliceCol: undefined,
    });
  });
});
