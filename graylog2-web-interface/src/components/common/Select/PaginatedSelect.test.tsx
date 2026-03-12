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
import React from 'react';
import { render, screen, act } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import PaginatedSelect from './PaginatedSelect';

const mockOptions = {
  list: [
    { label: 'Alpha', value: 'alpha' },
    { label: 'Beta', value: 'beta' },
  ],
  pagination: { page: 1, perPage: 50, query: '' },
  total: 2,
};

describe('PaginatedSelect', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should not crash when typing after debounced search fires', async () => {
    // The debounced handleSearch callback previously returned the Promise from
    // loadOptions. After the debounce fired, lodash's debounce wrapper would
    // return that Promise on subsequent calls. react-select treated the
    // non-null return value as a new inputValue, setting its internal state to
    // a Promise object, which crashed in trimString() with
    // "str.replace is not a function".
    const onLoadOptions = jest.fn(() => Promise.resolve(mockOptions));

    render(
      <PaginatedSelect
        placeholder="Pick one"
        onLoadOptions={onLoadOptions}
        onChange={() => {}}
      />,
    );

    // Wait for initial load
    await act(() => jest.runAllTimersAsync());

    const input = await screen.findByRole('combobox', { name: 'Pick one' });

    // Type first character — starts the 400ms debounce
    await userEvent.type(input, 'A', { advanceTimers: jest.advanceTimersByTimeAsync });

    // Fire the debounce so loadOptions runs and the debounced wrapper caches
    // its Promise return value
    await act(() => jest.advanceTimersByTimeAsync(500));

    // Type another character — the debounced wrapper now returns the cached
    // Promise. Before the fix, react-select would use it as inputValue and
    // crash in trimString().
    await userEvent.type(input, 'l', { advanceTimers: jest.advanceTimersByTimeAsync });

    await act(() => jest.advanceTimersByTimeAsync(500));

    // If we got here without throwing, the fix works.
    expect(input).toBeInTheDocument();
  });
});
