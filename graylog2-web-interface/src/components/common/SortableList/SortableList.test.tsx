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
  it('should list items', () => {
    render(<SortableList items={list} onMoveItem={() => {}} />);

    list.forEach((item) => expect(screen.getByText(item.title)).toBeInTheDocument());
  });

  it('should sort list', async () => {
    const onMoveItemStub = jest.fn();
    render(<SortableList items={list} onMoveItem={onMoveItemStub} />);

    const firstItem = screen.getByTestId('sortable-item-item-1');
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have lifted an item/i);
    fireEvent.keyDown(firstItem, { key: 'ArrowDown', keyCode: 40 });
    await screen.findByText(/You have moved the item/i);
    fireEvent.keyDown(firstItem, { key: 'Space', keyCode: 32 });
    await screen.findByText(/You have dropped the item/i);

    await waitFor(() => expect(onMoveItemStub).toHaveBeenCalledTimes(1));

    expect(onMoveItemStub).toHaveBeenCalledWith([
      { id: 'item-2', title: 'Item 2' },
      { id: 'item-1', title: 'Item 1' },
      { id: 'item-3', title: 'Item 3' },
    ], 0, 1);
  });

  it('should render list items with custom content', () => {
    render(<SortableList items={list} onMoveItem={() => {}} customContentRender={({ item: { id } }) => `Id: ${id}`} />);

    list.forEach((item) => expect(screen.getByText(`Id: ${item.id}`)).toBeInTheDocument());
  });

  it('should render custom list items', () => {
    const customListItemRender = (item, ref, className, dragHandleProps, draggableProps) => (
      <div ref={ref} className={className} {...dragHandleProps} {...draggableProps}>
        Id: {item.id}
      </div>
    );

    render(<SortableList items={list}
                         onMoveItem={() => {}}
                         customListItemRender={({ item, ref, className, dragHandleProps, draggableProps }) => customListItemRender(item, ref, className, dragHandleProps, draggableProps)} />);

    list.forEach((item) => expect(screen.getByText(`Id: ${item.id}`)).toBeInTheDocument());
  });
});
