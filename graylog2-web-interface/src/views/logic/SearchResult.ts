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
import type { List, Map } from 'immutable';
import { fromJS } from 'immutable';
import { mapValues, compact } from 'lodash';

import type { QueryId } from 'views/logic/queries/Query';
import type { SearchTypeId } from 'views/logic/SearchType';
import type { SearchJobResult } from 'views/stores/SearchStore';

import QueryResult from './QueryResult';
import type { SearchErrorResponse } from './SearchError';
import SearchError from './SearchError';
import type { ResultWindowLimitErrorResponse } from './ResultWindowLimitError';
import ResultWindowLimitError, {
  isResultWindowLimitErrorResponse,
} from './ResultWindowLimitError';

class SearchResult {
  private readonly _result: Map<string, any>;

  private readonly _results: Map<QueryId, QueryResult>;

  private readonly _errors: List<SearchError>;

  constructor(result: SearchJobResult) {
    this._result = fromJS(result);

    this._results = fromJS(mapValues(result.results, (queryResult) => new QueryResult(queryResult)));

    this._errors = fromJS(result?.errors ?? [].map((error: SearchErrorResponse | ResultWindowLimitErrorResponse) => {
      if (isResultWindowLimitErrorResponse(error)) {
        return new ResultWindowLimitError(error, this);
      }

      return new SearchError(error);
    }));
  }

  get result(): SearchJobResult {
    return this._result.toJS();
  }

  get results(): Record<QueryId, QueryResult> {
    return this._results.toJS();
  }

  get errors(): Array<SearchError> {
    return this._errors.toJS();
  }

  forId(queryId: QueryId) {
    return this._results.get(queryId);
  }

  updateSearchTypes(searchTypeResults) {
    const updatedResult = this.result;

    searchTypeResults.forEach((searchTypeResult: { id: string; }) => {
      const searchQuery = this._getQueryBySearchTypeId(searchTypeResult.id);

      updatedResult.results[searchQuery.query.id].search_types[searchTypeResult.id] = searchTypeResult;
    });

    return new SearchResult(updatedResult);
  }

  getSearchTypesFromResponse(searchTypeIds: Array<SearchTypeId>) {
    const searchTypes = searchTypeIds.map((searchTypeId) => {
      const relatedQuery = this._getQueryBySearchTypeId(searchTypeId);

      return SearchResult._getSearchTypeFromQuery(relatedQuery, searchTypeId);
    });

    return SearchResult._filterFailedSearchTypes(searchTypes);
  }

  _getQueryBySearchTypeId(searchTypeId: SearchTypeId) {
    return Object.values(this.result.results).find((query) => SearchResult._getSearchTypeFromQuery(query, searchTypeId));
  }

  private static _getSearchTypeFromQuery(query, searchTypeId: SearchTypeId) {
    return (query && query.search_types) ? query.search_types[searchTypeId] : undefined;
  }

  private static _filterFailedSearchTypes(searchTypes) {
    return compact(searchTypes);
  }
}

export default SearchResult;
