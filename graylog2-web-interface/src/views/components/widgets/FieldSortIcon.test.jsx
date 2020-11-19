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
// @flow strict
import React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import FieldSortIcon from './FieldSortIcon';

describe('FieldSortIcon', () => {
  const currentSort = new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Descending);
  const config = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [currentSort]);

  it('should set descending sort on click, if field sort is not defined', () => {
    const onSortChangeStub = jest.fn(() => Promise.resolve());
    const { getByTestId } = render(<FieldSortIcon config={config} fieldName="source" onSortChange={onSortChangeStub} setLoadingState={() => {}} />);

    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    const expectedSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending)];

    expect(onSortChangeStub).toHaveBeenCalledTimes(1);
    expect(onSortChangeStub).toHaveBeenCalledWith(expectedSort);
  });

  it('should set ascending sort on click, if field sort is descending', () => {
    const onSortChangeStub = jest.fn(() => Promise.resolve());
    const { getByTestId } = render(<FieldSortIcon config={config} fieldName="timestamp" onSortChange={onSortChangeStub} setLoadingState={() => {}} />);

    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    const expectedSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'timestamp', Direction.Ascending)];

    expect(onSortChangeStub).toHaveBeenCalledTimes(1);
    expect(onSortChangeStub).toHaveBeenCalledWith(expectedSort);
  });

  it('should set ascending sort on click, if field sort is descending', () => {
    const initialSort = new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Ascending);
    const initialConfig = new MessagesWidgetConfig(['timestamp', 'source'], true, [], [initialSort]);
    const onSortChangeStub = jest.fn(() => Promise.resolve());

    const { getByTestId } = render(<FieldSortIcon config={initialConfig} fieldName="source" onSortChange={onSortChangeStub} setLoadingState={() => {}} />);

    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    const expectedSort = [new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending)];

    expect(onSortChangeStub).toHaveBeenCalledTimes(1);
    expect(onSortChangeStub).toHaveBeenCalledWith(expectedSort);
  });

  it('should set loading state while changing sort', async () => {
    const onSortChangeStub = jest.fn(() => Promise.resolve());
    const setLoadingStateStub = jest.fn();
    const { getByTestId } = render(<FieldSortIcon config={config} fieldName="source" onSortChange={onSortChangeStub} setLoadingState={setLoadingStateStub} />);

    const sortIcon = getByTestId('messages-sort-icon');

    fireEvent.click(sortIcon);

    expect(setLoadingStateStub).toHaveBeenCalledTimes(1);
    expect(setLoadingStateStub).toHaveBeenCalledWith(true);

    await waitFor(() => expect(setLoadingStateStub).toHaveBeenCalledWith(false));

    expect(setLoadingStateStub).toHaveBeenCalledTimes(2);
  });
});
