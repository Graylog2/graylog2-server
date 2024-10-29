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

import QueryValidationActions from 'views/actions/QueryValidationActions';
import { validationError } from 'fixtures/queryValidationState';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';

import ViewsQueryInput from './ViewsQueryInput';

jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('hooks/useHotkey', () => jest.fn());

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
      <ViewsQueryInput value=""
                       name="search-query"
                       onChange={() => Promise.resolve('')}
                       validate={() => Promise.resolve({})}
                       isValidating={false}
                       onExecute={() => {}}
                       completerFactory={() => new Completer()}
                       {...props} />
    </TestStoreProvider>
  );

  useViewsPlugin();

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

    userEvent.paste(await findQueryInput(), 'the query');

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith({ target: { value: 'the query', name: 'search-query' } });
  });

  it('triggers onBlur when input is blurred', async () => {
    const onBlur = jest.fn();
    render(<SimpleQueryInput onBlur={onBlur} />);

    userEvent.paste(await findQueryInput(), 'the query');
    userEvent.tab();

    expect(onBlur).toHaveBeenCalledTimes(1);
  });

  describe('execution', () => {
    it('triggers onExecute on enter', async () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="the query" onExecute={onExecute} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(onExecute).toHaveBeenCalledTimes(1);
      expect(onExecute).toHaveBeenCalledWith('the query');
    });

    it('does not trigger onExecute on enter when execution is disabled', async () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="the query" onExecute={onExecute} disableExecution />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(onExecute).not.toHaveBeenCalledTimes(1);
    });

    it('triggers QueryValidationActions.displayValidationErrors when there in an error', async () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="source:" onExecute={onExecute} error={validationError} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(QueryValidationActions.displayValidationErrors).toHaveBeenCalledTimes(1);

      expect(onExecute).not.toHaveBeenCalled();
    });

    it('triggers QueryValidationActions.displayValidationErrors when there is an error after a change', async () => {
      const onExecute = jest.fn();
      const { rerender } = render(<SimpleQueryInput value="source" onExecute={onExecute} />);
      rerender(<SimpleQueryInput value="source:" onExecute={onExecute} error={validationError} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(QueryValidationActions.displayValidationErrors).toHaveBeenCalledTimes(1);

      expect(onExecute).not.toHaveBeenCalled();
    });
  });

  describe('supports custom commands', () => {
    it('adds custom commands to ace', async () => {
      const exec = jest.fn();
      const commands = [{
        name: 'TestCommand',
        bindKey: {
          mac: 'Ctrl+Enter',
          win: 'Ctrl+Enter',
        },
        exec,
      }];

      render(<SimpleQueryInput commands={commands} />);

      const queryInput = await findQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{ctrl}{enter}');

      await waitFor(() => {
        expect(exec).toHaveBeenCalled();
      });
    });
  });
});
