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
import asMock from 'helpers/mocking/AsMock';

import fetch from 'logic/rest/FetchProvider';
import Search from 'views/logic/search/Search';

import { SearchActions } from './SearchStore';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('SearchStore', () => {
  it('assigns a new search id when creating a search', () => {
    asMock(fetch).mockImplementation((method: string, url: string, body: any) => Promise.resolve(body && JSON.parse(body)));
    const newSearch = Search.create();

    return SearchActions.create(newSearch).then(({ search }) => {
      expect(search.id).not.toEqual(newSearch.id);
    });
  });
});
