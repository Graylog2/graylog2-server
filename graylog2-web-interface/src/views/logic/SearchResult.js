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
import { fromJS } from 'immutable';
import { mapValues, get, compact } from 'lodash';

import QueryResult from './QueryResult';
import SearchError from './SearchError';
import ResultWindowLimitError from './ResultWindowLimitError';

class SearchResult {
  constructor(result) {
    this._result = fromJS(result);

    this._results = fromJS(mapValues(result.results, (queryResult) => new QueryResult(queryResult)));

    this._errors = fromJS(get(result, 'errors', []).map((error) => {
      if (error.type === 'result_window_limit') {
        return new ResultWindowLimitError(error, this);
      }

      return new SearchError(error);
    }));
  }

  get result() {
    return this._result.toJS();
  }

  get results() {
    return this._results.toJS();
  }

  get errors() {
    return this._errors.toJS();
  }

  forId(queryId) {
    return this._results.get(queryId);
  }

  updateSearchTypes(searchTypeResults) {
    const updatedResult = this.result;

    searchTypeResults.forEach((searchTypeResult) => {
      const searchQuery = this._getQueryBySearchTypeId(searchTypeResult.id);

      updatedResult.results[searchQuery.query.id].search_types[searchTypeResult.id] = searchTypeResult;
    });

    return new SearchResult(updatedResult);
  }

  getSearchTypesFromResponse(searchTypeIds) {
    const searchTypes = searchTypeIds.map((searchTypeId) => {
      const relatedQuery = this._getQueryBySearchTypeId(searchTypeId);

      return SearchResult._getSearchTypeFromQuery(relatedQuery, searchTypeId);
    });

    return SearchResult._filterFailedSearchTypes(searchTypes);
  }

  _getQueryBySearchTypeId(searchTypeId) {
    return Object.values(this.result.results).find((query) => SearchResult._getSearchTypeFromQuery(query, searchTypeId));
  }

  static _getSearchTypeFromQuery(query, searchTypeId) {
    return (query && query.search_types) ? query.search_types[searchTypeId] : undefined;
  }

  static _filterFailedSearchTypes(searchTypes) {
    return compact(searchTypes);
  }
}

export default SearchResult;
