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

import QueryValidationActions from 'views/actions/QueryValidationActions';
import { validationError } from 'fixtures/queryValidationState';

import QueryInput from './QueryInput';

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: {
    getInitialState: jest.fn(),
    listen: jest.fn(),
  },
}));

jest.mock('views/actions/QueryValidationActions', () => ({
  displayValidationErrors: jest.fn(),
}));

class Completer {
  getCompletions = (editor, session, pos, prefix, callback) => {
    callback(null, []);
  }
}

describe('QueryInput', () => {
  const getQueryInput = () => screen.getByRole('textbox');

  const SimpleQueryInput = (props) => (
    <QueryInput value=""
                onChange={() => Promise.resolve('')}
                onExecute={() => {}}
                completerFactory={() => new Completer()}
                {...props} />
  );

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders with minimal props', () => {
    render(<SimpleQueryInput />);

    expect(getQueryInput()).toBeInTheDocument();
  });

  it('triggers onChange when input is changed', () => {
    const onChange = jest.fn();
    render(<SimpleQueryInput onChange={onChange} />);

    userEvent.paste(getQueryInput(), 'the query');

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith('the query', expect.any(Object));
  });

  it('triggers onBlur when input is blurred', () => {
    const onBlur = jest.fn();
    render(<SimpleQueryInput onBlur={onBlur} />);

    userEvent.paste(getQueryInput(), 'the query');
    userEvent.tab();

    expect(onBlur).toHaveBeenCalledTimes(1);
  });

  describe('on submit', () => {
    it('triggers onExecute on enter', () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="the query" onExecute={onExecute} />);

      const queryInput = getQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(onExecute).toHaveBeenCalledTimes(1);
      expect(onExecute).toHaveBeenCalledWith('the query');
    });

    it('triggers QueryValidationActions.displayValidationErrors when there in an error', () => {
      const onExecute = jest.fn();
      render(<SimpleQueryInput value="source:" onExecute={onExecute} error={validationError} />);

      const queryInput = getQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(QueryValidationActions.displayValidationErrors).toHaveBeenCalledTimes(1);

      expect(onExecute).not.toHaveBeenCalled();
    });

    it('triggers QueryValidationActions.displayValidationErrors when there is an error after a change', () => {
      const onExecute = jest.fn();
      const { rerender } = render(<SimpleQueryInput value="source" onExecute={onExecute} />);
      rerender(<SimpleQueryInput value="source:" onExecute={onExecute} error={validationError} />);

      const queryInput = getQueryInput();
      queryInput.focus();
      userEvent.type(queryInput, '{enter}');

      expect(QueryValidationActions.displayValidationErrors).toHaveBeenCalledTimes(1);

      expect(onExecute).not.toHaveBeenCalled();
    });
  });
});
