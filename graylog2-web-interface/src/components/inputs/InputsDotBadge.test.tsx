import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import type { InputStateSummary } from 'hooks/useInputsStates';
import useInputsStates from 'hooks/useInputsStates';

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
        states: [
          { id: '1', state: 'RUNNING' } as InputStateSummary,
          { id: '2', state: 'STARTING' } as InputStateSummary,
        ],
      },
    });

    render(<InputsDotBadge text={TEXT} />);

    const textEl = screen.getByText(TEXT);
    expect(textEl).toBeInTheDocument();
    expect(textEl).not.toHaveAttribute('title', 'Some inputs are in failed state or in setup mode.');
  });

  describe.each(['FAILED', 'FAILING', 'SETUP'])('renders badge when an input state is %s', (problemState) => {
    it(`shows badge (dot) with tooltip for state ${problemState}`, () => {
      asMock(useInputsStates).mockReturnValue({
        refetch: jest.fn(),
        isLoading: false,
        data: {
          states: [
            { id: '1', state: 'RUNNING' } as InputStateSummary,
            { id: '2', state: problemState } as InputStateSummary,
          ],
        },
      });

      render(<InputsDotBadge text={TEXT} />);

      const badge = screen.getByTitle(/Some inputs are in failed state or in setup mode\./i);
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveTextContent(TEXT);
    });
  });
});
