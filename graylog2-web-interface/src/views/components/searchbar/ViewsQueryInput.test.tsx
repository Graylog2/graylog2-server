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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import QueryValidationActions from 'views/actions/QueryValidationActions';
import { validationError } from 'fixtures/queryValidationState';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import asMock from 'helpers/mocking/AsMock';
import { fetchRawQueryHistory } from 'views/components/searchbar/QueryHistoryButton';

import ViewsQueryInput from './ViewsQueryInput';

jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('hooks/useHotkey', () => jest.fn());
jest.mock('views/components/searchbar/QueryHistoryButton', () => ({
  ...jest.requireActual('views/components/searchbar/QueryHistoryButton'),
  fetchRawQueryHistory: jest.fn(),
  displayHistoryCompletions: jest.fn(),
}));

jest.mock('views/actions/QueryValidationActions', () => ({
  displayValidationErrors: jest.fn(),
}));

class Completer {
  // eslint-disable-next-line class-methods-use-this
  getCompletions = (_editor, _session, _pos, _prefix, callback) => {
    callback(null, []);
  };

  shouldShowCompletions: () => true;
}

describe('QueryInput', () => {
  const findQueryInput = () => screen.findByRole('textbox');

  const SimpleQueryInput = (props: Partial<React.ComponentProps<typeof ViewsQueryInput>>) => (
    <TestStoreProvider>
      <ViewsQueryInput
        value=""
        name="search-query"
        onChange={() => Promise.resolve('')}
        validate={() => Promise.resolve({})}
        isValidating={false}
        onExecute={() => {}}
        completerFactory={() => new Completer()}
        {...props}
      />
    </TestStoreProvider>
  );

  useViewsPlugin();

  beforeEach(() => {
    asMock(fetchRawQueryHistory).mockResolvedValue([]);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders with minimal props', async () => {
    render(<SimpleQueryInput />);

    expect(await findQueryInput()).toBeInTheDocument();
  });

  it('triggers onChange when input is changed', async () => {
    const onChange = jest.fn();
    render(<SimpleQueryInput onChange={onChange} />);

    const queryInput = await findQueryInput();
    // eslint-disable-next-line testing-library/prefer-user-event
    fireEvent.input(queryInput, { target: { value: 'the query' } });

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith({ target: { value: 'the query', name: 'search-query' } });
  });

  it('triggers onBlur when input is blurred', async () => {
    const onBlur = jest.fn();
    render(<SimpleQueryInput onBlur={onBlur} />);

    const queryInput = await findQueryInput();
    queryInput.focus();
    // eslint-disable-next-line testing-library/prefer-user-event
    fireEvent.input(queryInput, { target: { value: 'the query' } });
    await userEvent.tab();

    expect(onBlur).toHaveBeenCalledTimes(1);
  });

  describe('execution', () => {
    it('triggers onExecute on enter', async () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="the query" onExecute={onExecute} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.type(queryInput, '{enter}');

      expect(onExecute).toHaveBeenCalledTimes(1);
      expect(onExecute).toHaveBeenCalledWith('the query');
    });

    it('does not trigger onExecute on enter when execution is disabled', async () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="the query" onExecute={onExecute} disableExecution />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.type(queryInput, '{enter}');

      expect(onExecute).not.toHaveBeenCalledTimes(1);
    });

    it('triggers QueryValidationActions.displayValidationErrors when there in an error', async () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="source:" onExecute={onExecute} error={validationError} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.type(queryInput, '{enter}');

      expect(QueryValidationActions.displayValidationErrors).toHaveBeenCalledTimes(1);

      expect(onExecute).not.toHaveBeenCalled();
    });

    it('triggers QueryValidationActions.displayValidationErrors when there is an error after a change', async () => {
      const onExecute = jest.fn();
      const { rerender } = render(<SimpleQueryInput value="source" onExecute={onExecute} />);
      rerender(<SimpleQueryInput value="source:" onExecute={onExecute} error={validationError} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.type(queryInput, '{enter}');

      expect(QueryValidationActions.displayValidationErrors).toHaveBeenCalledTimes(1);

      expect(onExecute).not.toHaveBeenCalled();
    });
  });

  describe('supports custom commands', () => {
    it('adds custom commands to ace', async () => {
      const exec = jest.fn();
      const commands = [
        {
          name: 'TestCommand',
          bindKey: {
            mac: 'Ctrl+Enter',
            win: 'Ctrl+Enter',
          },
          exec,
        },
      ];

      render(<SimpleQueryInput commands={commands} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{Control>}{Enter}{/Control}');

      await waitFor(() => {
        expect(exec).toHaveBeenCalled();
      });
    });
  });

  describe('history navigation', () => {
    beforeEach(() => {
      asMock(fetchRawQueryHistory).mockResolvedValue(['query-1', 'query-2', 'query-3']);
    });

    it('shows the most recent history entry on first ArrowUp when the current value differs', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      render(<SimpleQueryInput onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowUp}');

      await waitFor(() => {
        expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } });
      });
    });

    it('skips history[0] on first ArrowUp when it matches the current value', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      // Current value matches the top history entry.
      render(<SimpleQueryInput value="query-1" onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowUp}');

      await waitFor(() => {
        expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-2', name: 'search-query' } });
      });
    });

    it('advances to the next older entry on a second ArrowUp', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      render(<SimpleQueryInput onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } }));

      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => {
        expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-2', name: 'search-query' } });
      });
    });

    it('moves back toward present on ArrowDown after ArrowUp', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      render(<SimpleQueryInput onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } }));

      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-2', name: 'search-query' } }));

      await userEvent.keyboard('{ArrowDown}');
      await waitFor(() => {
        expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } });
      });
    });

    it('restores the original query when ArrowDown is pressed past the most recent entry', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      render(<SimpleQueryInput value="original" onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } }));

      await userEvent.keyboard('{ArrowDown}');
      await waitFor(() => {
        expect(onChange).toHaveBeenCalledWith({ target: { value: 'original', name: 'search-query' } });
      });
    });

    it('restores in one Down press when history[0] was skipped going up', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      // Current value matches history[0], so Up skips to history[1].
      render(<SimpleQueryInput value="query-1" onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-2', name: 'search-query' } }));

      // One Down should restore directly, not stop at the skipped history[0].
      await userEvent.keyboard('{ArrowDown}');
      await waitFor(() => {
        expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } });
      });
    });

    it('does not navigate history when ArrowDown is pressed without prior ArrowUp', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      render(<SimpleQueryInput onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      await userEvent.keyboard('{ArrowDown}');

      expect(onChange).not.toHaveBeenCalled();
    });

    it('clamps at the oldest history entry and does not wrap around', async () => {
      const onChange = jest.fn().mockResolvedValue('');
      render(<SimpleQueryInput onChange={onChange} />);

      const queryInput = await findQueryInput();
      queryInput.focus();

      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-1', name: 'search-query' } }));
      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-2', name: 'search-query' } }));
      await userEvent.keyboard('{ArrowUp}');
      await waitFor(() => expect(onChange).toHaveBeenCalledWith({ target: { value: 'query-3', name: 'search-query' } }));

      const callsBefore = onChange.mock.calls.length;

      await userEvent.keyboard('{ArrowUp}');

      expect(onChange.mock.calls.length).toBe(callsBefore);
    });
  });
});
