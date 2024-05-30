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
// eslint-disable-next-line no-restricted-imports
import type { DebouncedFunc } from 'lodash';
import { fireEvent, render, screen, within, waitFor } from 'wrappedTestingLibrary';
import debounce from 'lodash/debounce';
import Immutable from 'immutable';
import { Formik } from 'formik';

import { asMock } from 'helpers/mocking';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import mockSearchesClusterConfig from 'fixtures/searchClusterConfig';

import TimeRangePresetForm from './TimeRangePresetForm';

jest.mock('hooks/useSearchConfiguration', () => jest.fn());
jest.mock('lodash/debounce', () => jest.fn());
jest.mock('logic/generateId', () => jest.fn(() => 'tr-id-3'));
jest.mock('hooks/useHotkey', () => jest.fn());

const mockOnUpdate = jest.fn();

const renderForm = () => render(
  <Formik initialValues={{ selectedFields: [] }} onSubmit={() => {}}>
    <TimeRangePresetForm options={Immutable.List([
      { description: 'TimeRange1', id: 'tr-id-1', timerange: { from: 300, type: 'relative' } },
      { description: 'TimeRange2', id: 'tr-id-2', timerange: { from: 600, type: 'relative' } },
    ])}
                         onUpdate={mockOnUpdate} />
  </Formik>);

describe('TimeRangePresetForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSearchConfiguration).mockReturnValue({
      config: mockSearchesClusterConfig,
      refresh: jest.fn(),
    });

    asMock(debounce as DebouncedFunc<(...args: any) => any>).mockImplementation((fn) => fn);
  });

  it('render all items', async () => {
    renderForm();
    await screen.findByText('5 minutes ago');
    await screen.findByText('10 minutes ago');
  });

  it('remove action trigger onUpdate', async () => {
    renderForm();
    const timerangeItem = await screen.findByTestId('time-range-preset-tr-id-1');
    const removeButton = await within(timerangeItem).findByTitle('Remove preset');

    fireEvent.click(removeButton);

    expect(mockOnUpdate).toHaveBeenCalledWith(Immutable.List([
      { description: 'TimeRange2', id: 'tr-id-2', timerange: { from: 600, type: 'relative' } },
    ]));
  });

  it('add action trigger onUpdate', async () => {
    renderForm();
    const addItemButton = await screen.findByLabelText('Add new search time range preset');

    fireEvent.click(addItemButton);

    expect(mockOnUpdate).toHaveBeenCalledWith(Immutable.List([
      { description: 'TimeRange1', id: 'tr-id-1', timerange: { from: 300, type: 'relative' } },
      { description: 'TimeRange2', id: 'tr-id-2', timerange: { from: 600, type: 'relative' } },
      { description: '', id: 'tr-id-3', timerange: { from: 300, type: 'relative' } },
    ]));
  });

  it('edit description action trigger onUpdate', async () => {
    renderForm();
    const timerangeItem = await screen.findByTestId('time-range-preset-tr-id-1');
    const descriptionInput = await within(timerangeItem).findByTitle('Time range preset description');

    fireEvent.change(descriptionInput, { target: { value: 'TimeRange1 changed' } });

    expect(mockOnUpdate).toHaveBeenCalledWith(Immutable.List([
      { description: 'TimeRange1 changed', id: 'tr-id-1', timerange: { from: 300, type: 'relative' } },
      { description: 'TimeRange2', id: 'tr-id-2', timerange: { from: 600, type: 'relative' } },
    ]));
  });

  it('edit time range action triggers onUpdate', async () => {
    renderForm();
    const timerangeItem = await screen.findByTestId('time-range-preset-tr-id-1');
    const timerangeFilter = await within(timerangeItem).findByText('5 minutes ago');
    fireEvent.click(timerangeFilter);
    const fromInput = await screen.findByTitle('Set the from value');
    fireEvent.change(fromInput, { target: { value: 15 } });
    const submitButton = await screen.findByRole('button', { name: /update time range/i });

    await waitFor(() => expect(submitButton).toBeEnabled());
    fireEvent.click(submitButton);

    await waitFor(() => expect(mockOnUpdate).toHaveBeenCalledWith(Immutable.List([
      { description: 'TimeRange1', id: 'tr-id-1', timerange: { from: 900, type: 'relative' } },
      { description: 'TimeRange2', id: 'tr-id-2', timerange: { from: 600, type: 'relative' } },
    ])));
  });
});
