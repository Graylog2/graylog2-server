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
import { render, screen } from 'wrappedTestingLibrary';
import { waitFor, act } from 'wrappedTestingLibrary/hooks';
import userEvent from '@testing-library/user-event';

import SortableList from './SortableList';
import useSortableItemRectsMock from './tests/useSortableItemRectsMock';

const list = [
  { id: 'item-1', title: 'Item 1' },
  { id: 'item-2', title: 'Item 2' },
  { id: 'item-3', title: 'Item 3' },
];

describe('SortableList', () => {
  useSortableItemRectsMock({ height: 10, width: 100 });

  it('should list items', async () => {
    render(<SortableList items={list} onMoveItem={() => {}} />);

    await screen.findByText('Item 1');
    await screen.findByText('Item 2');
    await screen.findByText('Item 3');
  });

  it('should sort list', async () => {
    const onMoveItemStub = jest.fn();
    render(<SortableList items={list} onMoveItem={onMoveItemStub} />);

    const firstItemDragHandle = await screen.findByRole('button', {
      name: /Drag or press space to reorder Item 1/i,
    });
    firstItemDragHandle.focus();

    await act(async () => {
      await userEvent.keyboard('[Space]');
    });

    userEvent.keyboard('{ArrowDown}');

    await screen.findByText('Draggable item item-1 was moved over droppable area item-2.');

    await act(async () => {
      await userEvent.keyboard('[Space]');
    });

    await waitFor(() =>
      expect(onMoveItemStub).toHaveBeenCalledWith(
        [
          { id: 'item-2', title: 'Item 2' },
          { id: 'item-1', title: 'Item 1' },
          { id: 'item-3', title: 'Item 3' },
        ],
        0,
        1,
      ),
    );
  });

  it('should render list items with custom content', () => {
    render(<SortableList items={list} onMoveItem={() => {}} customContentRender={({ item: { id } }) => `Id: ${id}`} />);

    list.forEach((item) => expect(screen.getByText(`Id: ${item.id}`)).toBeInTheDocument());
  });

  it('should render custom list items', () => {
    const customListItemRender = (item, ref, className, dragHandle) => (
      <div ref={ref} className={className}>
        {dragHandle}
        Id: {item.id}
      </div>
    );

    render(
      <SortableList
        items={list}
        onMoveItem={() => {}}
        customListItemRender={({ item, ref, className, dragHandle }) =>
          customListItemRender(item, ref, className, dragHandle)
        }
      />,
    );

    list.forEach((item) => expect(screen.getByText(`Id: ${item.id}`)).toBeInTheDocument());
  });
});
