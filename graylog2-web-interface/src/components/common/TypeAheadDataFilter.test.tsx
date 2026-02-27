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
import userEvent from '@testing-library/user-event';
import { act, render, screen } from 'wrappedTestingLibrary';

import TypeAheadDataFilter from './TypeAheadDataFilter';

describe('<TypeAheadDataFilter />', () => {
  const data = [{ name: 'Alpha' }, { name: 'Beta' }, { name: 'Gamma' }];

  const renderSut = (props = {}) => {
    const onDataFiltered = jest.fn();

    render(
      <TypeAheadDataFilter
        id="filter"
        label="Filter"
        data={data}
        searchInKeys={['name']}
        onDataFiltered={onDataFiltered}
        debounceMs={200}
        {...props}
      />,
    );

    return { onDataFiltered };
  };

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('filters data after debounce', async () => {
    const { onDataFiltered } = renderSut();
    const input = screen.getByRole('textbox', { name: /filter/i });

    await userEvent.type(input, 'be');

    act(() => {
      jest.advanceTimersByTime(200);
    });

    expect(onDataFiltered).toHaveBeenCalledTimes(1);
    expect(onDataFiltered).toHaveBeenCalledWith([{ name: 'Beta' }], 'be');
  });

  it('flushes filter on Enter and resets', async () => {
    const { onDataFiltered } = renderSut({ debounceMs: 1000 });
    const input = screen.getByRole('textbox', { name: /filter/i });
    const resetButton = screen.getByRole('button', { name: /reset/i });

    expect(resetButton).toBeDisabled();

    await userEvent.type(input, 'ga');
    expect(resetButton).toBeEnabled();

    await userEvent.keyboard('{Enter}');

    expect(onDataFiltered).toHaveBeenCalledTimes(1);
    expect(onDataFiltered).toHaveBeenCalledWith([{ name: 'Gamma' }], 'ga');

    await userEvent.click(resetButton);

    expect(onDataFiltered).toHaveBeenCalledTimes(2);
    expect(onDataFiltered).toHaveBeenLastCalledWith(data, '');
  });
});
