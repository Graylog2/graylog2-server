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

/* eslint-disable import/prefer-default-export */

import { Map } from 'immutable';

import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';

export const createSearch = ({ searchId, queryId }: { searchId?: string, queryId?: string} = {}) => {
  const exampleSearchId = searchId ?? 'search-id-1';
  const exampleQueryId = queryId ?? 'query-id-1';

  const viewState = ViewState.builder().build();
  const query = Query.builder().id(exampleQueryId).build();
  const searchSearch = Search.builder().queries([query]).id(exampleSearchId).build();

  return View.builder()
    .search(searchSearch)
    .type(View.Type.Dashboard)
    .state(Map({ [exampleQueryId]: viewState }))
    .id(exampleSearchId)
    .title('Search 1')
    .build();
};
