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

import type { PaginatedListType, DescriptiveItem } from './PaginatedItemOverview';
import PaginatedItemOverview from './PaginatedItemOverview';

const examplePaginatedResponse: PaginatedListType = {
  list: Immutable.List<DescriptiveItem>([]),
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

    resolvePromise(examplePaginatedResponse);

    expect(await screen.findByText(/no items found to display/i)).toBeInTheDocument();
  });
});
