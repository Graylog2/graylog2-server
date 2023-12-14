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

import * as React from 'react';
import { useMemo } from 'react';

import type { SearchJson } from 'views/logic/search/Search';
import type { SearchJobResult } from 'views/logic/SearchResult';
import SearchExecutorsContext from 'views/components/contexts/SearchExecutorsContext';
import SearchPage from 'views/pages/SearchPage';
import Search from 'views/logic/search/Search';
import type { ViewJson } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import SearchResult from 'views/logic/SearchResult';
import type { SearchExecutors } from 'views/logic/slices/searchExecutionSlice';
import SearchMetadata from 'views/logic/search/SearchMetadata';

type Props = {
  searchJson: Partial<SearchJson>,
  viewJson: any,
  searchJobResult: Partial<SearchJobResult>,
}
const searchMetadata = SearchMetadata.empty();

const StaticSearch = ({ searchJson, viewJson, searchJobResult }: Props) => {
  const view = useMemo(() => {
    const search = Search.fromJSON(searchJson as SearchJson);

    return View.fromJSON(viewJson as ViewJson)
      .toBuilder()
      .search(search)
      .build();
  }, [searchJson, viewJson]);

  const searchResult = useMemo(() => ({
    result: new SearchResult(searchJobResult as SearchJobResult),
    widgetMapping: view.widgetMapping,
  }), [searchJobResult, view.widgetMapping]);

  const searchExecutors: SearchExecutors = useMemo(() => ({
    execute: async () => searchResult,
    parse: async () => searchMetadata,
    resultMapper: (result) => result,
  }), [searchResult]);

  return (
    <SearchExecutorsContext.Provider value={searchExecutors}>
      <SearchPage view={Promise.resolve(view)} isNew={false} searchResult={searchResult} />
    </SearchExecutorsContext.Provider>
  );
};

export default StaticSearch;
