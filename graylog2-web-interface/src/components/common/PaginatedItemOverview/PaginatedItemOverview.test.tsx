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
import { screen, render } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import type {
  PaginatedListType,
  DescriptiveItem,
  OverrideItemComponentProps,
  ResultsWrapperComponentProps,
} from './PaginatedItemOverview';
import PaginatedItemOverview from './PaginatedItemOverview';

const emptyPaginatedResponse: PaginatedListType = {
  list: Immutable.List<DescriptiveItem>([]),
  pagination: {
    query: '',
    page: 1,
    perPage: 0,
    total: 1,
    count: 1,
  },
};

const simplePaginatedResponse: PaginatedListType = {
  list: Immutable.List<DescriptiveItem>([{
    id: '1',
    name: 'Foo',
    description: 'Bar',
  }]),
  pagination: {
    query: '',
    page: 1,
    perPage: 0,
    total: 1,
    count: 1,
  },
};

describe('<PaginatedItemOverview>', () => {
  it('fetches items through onLoad function', async () => {
    let resolvePromise;
    const mockPromise = new Promise<PaginatedListType>((resolve) => {
      resolvePromise = resolve;
    });
    const loadHandler = jest.fn(() => mockPromise);

    render(<PaginatedItemOverview onLoad={loadHandler} />);

    await screen.findByText(/loading/i);

    expect(loadHandler).toHaveBeenCalledTimes(1);

    resolvePromise(emptyPaginatedResponse);

    await screen.findByText(/no items found to display/i);
  });

  it('uses custom result wrapper', async () => {
    const myWrapper = ({ children, isEmptyResult }: ResultsWrapperComponentProps) => (
      <ul>
        <li>My custom wrapper</li>
        <li>Empty result {JSON.stringify(isEmptyResult)}</li>
        <li>{children}</li>
      </ul>
    );

    render(
      <PaginatedItemOverview onLoad={() => Promise.resolve({} as PaginatedListType)}
                             overrideList={emptyPaginatedResponse}
                             resultsWrapperComponent={myWrapper} />,
    );

    await screen.findByText(/my custom wrapper/i);
    await screen.findByText(/no items found to display/i);
    await screen.findByText(/empty result true/i);
  });

  it('uses default item component', async () => {
    const { rerender } = render(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)} />,
    );

    const itemName = simplePaginatedResponse.list.get(0).name;

    await screen.findByText(itemName, { exact: false });

    expect(screen.queryByTitle(`Remove ${itemName}`)).not.toBeInTheDocument();

    rerender(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)}
                             onDeleteItem={jest.fn()} />,
    );

    await screen.findByText(simplePaginatedResponse.list.get(0).name, { exact: false });
    await screen.findByTitle(`Remove ${itemName}`);
  });

  it('uses custom item component', async () => {
    const itemComponent = ({ item, onDeleteItem }: OverrideItemComponentProps) => (
      <ul>
        <li>Custom item component</li>
        <li>{item.name}</li>
        <li>{onDeleteItem && <button type="button">Delete</button>}</li>
      </ul>
    );

    const { rerender } = render(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)}
                             overrideList={simplePaginatedResponse}
                             overrideItemComponent={itemComponent} />,
    );

    await screen.findByText(/custom item component/i);
    await screen.findByText(simplePaginatedResponse.list.get(0).name);

    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();

    rerender(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)}
                             overrideList={simplePaginatedResponse}
                             onDeleteItem={jest.fn()}
                             overrideItemComponent={itemComponent} />,
    );

    await screen.findByText(/custom item component/i);
    await screen.findByText(simplePaginatedResponse.list.get(0).name);
    await screen.findByRole('button', { name: 'Delete' });
  });
});
