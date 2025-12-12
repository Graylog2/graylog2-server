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

import { asMock } from 'helpers/mocking';
import type { InputState } from 'hooks/useInputsStates';
import useInputsStates from 'hooks/useInputsStates';
import type { InputSummary } from 'hooks/usePaginatedInputs';

import InputsDotBadge from './InputsDotBadge';

jest.mock('hooks/useInputsStates');

const TEXT = 'Inputs';

describe('<InputsDotBadge />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns null while loading', () => {
    asMock(useInputsStates).mockReturnValue({
      refetch: jest.fn(),
      isLoading: true,
      data: undefined,
    });

    render(<InputsDotBadge text={TEXT} />);

    expect(screen.queryByText(TEXT)).not.toBeInTheDocument();
  });

  it('renders plain text when there are no failed/failing/setup inputs', () => {
    asMock(useInputsStates).mockReturnValue({
      refetch: jest.fn(),
      isLoading: false,
      data: {
        input1: {
          nodeA: { state: 'RUNNING', id: '1', detailed_message: null, message_input: {} as InputSummary },
          nodeB: { state: 'STARTING', id: '2', detailed_message: 'Error', message_input: {} as InputSummary },
        },
        input2: {
          nodeC: { state: 'RUNNING', id: '3', detailed_message: null, message_input: {} as InputSummary },
        },
      },
    });

    render(<InputsDotBadge text={TEXT} />);

    const textEl = screen.getByText(TEXT);
    expect(textEl).toBeInTheDocument();
    expect(textEl).not.toHaveAttribute('title', 'Some inputs are in failed state or in setup mode.');
  });

  describe.each(['FAILED', 'FAILING', 'SETUP'])(
    'renders badge when an input state is %s',
    (problemState: InputState) => {
      it(`shows badge (dot) with tooltip for state ${problemState}`, () => {
        asMock(useInputsStates).mockReturnValue({
          refetch: jest.fn(),
          isLoading: false,
          data: {
            input1: {
              nodeA: { state: 'RUNNING', id: '1', detailed_message: null, message_input: {} as InputSummary },
              nodeB: { state: problemState, id: '2', detailed_message: 'Error', message_input: {} as InputSummary },
            },
          },
        });

        render(<InputsDotBadge text={TEXT} />);

        const badge = screen.getByTitle(/Some inputs are in failed state or in setup mode\./i);
        expect(badge).toBeInTheDocument();
        expect(badge).toHaveTextContent(TEXT);
      });
    },
  );
});
