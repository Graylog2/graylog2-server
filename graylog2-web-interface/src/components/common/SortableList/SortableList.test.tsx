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
import { render, fireEvent, screen, waitFor } from 'wrappedTestingLibrary';

import SortableList from './SortableList';

const list = [
  { id: 'item-1', title: 'Item 1' },
  { id: 'item-2', title: 'Item 2' },
  { id: 'item-3', title: 'Item 3' },
];

describe('SortableList', () => {
  it('should sort list', async () => {
    const onSortChangeStub = jest.fn();
    render(<SortableList items={list} onSortChange={onSortChangeStub} />);

    const firstItem = screen.getByTestId('sortable-item-item-1');
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have lifted an item/i);
    fireEvent.keyDown(firstItem, { key: 'ArrowDown', keyCode: 40 });
    await screen.findByText(/You have moved the item/i);
    fireEvent.keyDown(firstItem, { key: 'ArrowUp', keyCode: 32 });
    await screen.findByText(/You have dropped the item/i);

    await waitFor(() => expect(onSortChangeStub).toHaveBeenCalledTimes(1));

    expect(onSortChangeStub).toHaveBeenCalledWith([
      { id: 'item-2', title: 'Item 2' },
      { id: 'item-1', title: 'Item 1' },
      { id: 'item-3', title: 'Item 3' },
    ], 0, 1);
  });
});
