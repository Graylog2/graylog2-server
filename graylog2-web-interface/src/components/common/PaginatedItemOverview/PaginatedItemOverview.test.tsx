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
  ResultsWrapperProps,
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

    expect(await screen.findByText(/loading/i)).toBeInTheDocument();
    expect(loadHandler).toHaveBeenCalledTimes(1);

    resolvePromise(emptyPaginatedResponse);

    expect(await screen.findByText(/no items found to display/i)).toBeInTheDocument();
  });

  it('uses custom result wrapper', async () => {
    const myWrapper = ({ children, isEmptyResult }: ResultsWrapperProps) => (
      <ul>
        <li>My custom wrapper</li>
        <li>Empty result {JSON.stringify(isEmptyResult)}</li>
        <li>{children}</li>
      </ul>
    );

    render(
      <PaginatedItemOverview onLoad={() => Promise.resolve({} as PaginatedListType)}
                             overrideList={emptyPaginatedResponse}
                             resultsWrapper={myWrapper} />,
    );

    expect(await screen.findByText(/my custom wrapper/i)).toBeInTheDocument();
    expect(await screen.findByText(/no items found to display/i)).toBeInTheDocument();
    expect(await screen.findByText(/empty result true/i)).toBeInTheDocument();
  });

  it('uses default item component', async () => {
    const { rerender } = render(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)} />,
    );

    const itemName = simplePaginatedResponse.list.get(0).name;

    expect(await screen.findByText(itemName, { exact: false })).toBeInTheDocument();
    expect(screen.queryByTitle(`Remove ${itemName}`)).not.toBeInTheDocument();

    rerender(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)}
                             onDeleteItem={jest.fn()} />,
    );

    expect(await screen.findByText(simplePaginatedResponse.list.get(0).name, { exact: false })).toBeInTheDocument();
    expect(await screen.findByTitle(`Remove ${itemName}`)).toBeInTheDocument();
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

    expect(await screen.findByText(/custom item component/i)).toBeInTheDocument();
    expect(await screen.findByText(simplePaginatedResponse.list.get(0).name)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();

    rerender(
      <PaginatedItemOverview onLoad={() => Promise.resolve(simplePaginatedResponse)}
                             overrideList={simplePaginatedResponse}
                             onDeleteItem={jest.fn()}
                             overrideItemComponent={itemComponent} />,
    );

    expect(await screen.findByText(/custom item component/i)).toBeInTheDocument();
    expect(await screen.findByText(simplePaginatedResponse.list.get(0).name)).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Delete' })).toBeInTheDocument();
  });
});
