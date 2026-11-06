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
import { createSearch as createViewFixture } from 'fixtures/searches';
import type { RootState } from 'views/types';
import createSearch from 'views/logic/slices/createSearch';
import { mockSearchExecutors } from 'views/test/mockDispatch';

import { updateQueries } from './viewSlice';

jest.mock('views/logic/slices/createSearch', () => jest.fn(async (search) => search));

const view = createViewFixture();
const getState = () => ({ view: { view, activeQuery: 'query-id-1' } }) as RootState;

describe('viewSlice', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('stores a changed search when the search executors do not create searches themselves', async () => {
    await updateQueries(view.search.queries)(jest.fn(), getState, { searchExecutors: mockSearchExecutors });

    expect(createSearch).toHaveBeenCalledTimes(1);
  });

  it('creates a changed search with the search executors when they provide `createSearch`', async () => {
    const executorsCreateSearch = jest.fn(async (search) => search);

    await updateQueries(view.search.queries)(jest.fn(), getState, {
      searchExecutors: { ...mockSearchExecutors, createSearch: executorsCreateSearch },
    });

    expect(executorsCreateSearch).toHaveBeenCalledTimes(1);
    expect(createSearch).not.toHaveBeenCalled();
  });
});
