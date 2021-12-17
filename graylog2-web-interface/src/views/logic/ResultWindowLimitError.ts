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
import type { QueryId } from 'views/logic/queries/Query';
import type { SearchTypeId } from 'views/logic/SearchType';
import type SearchResult from 'views/logic/SearchResult';

import type { SearchErrorResponse, SearchErrorState } from './SearchError';
import SearchError from './SearchError';

type ResultWindowLimitErrorState = SearchErrorState & {};

export type ResultWindowLimitErrorResponse = SearchErrorResponse & {
  type: 'result_window_limit',
  result_window_limit: number,
};

export const isResultWindowLimitErrorResponse = (error: SearchErrorResponse): error is ResultWindowLimitErrorResponse => (error?.type === 'result_window_limit');

export default class ResultWindowLimitError extends SearchError {
  protected readonly _state: ResultWindowLimitErrorState;

  private readonly _resultWindowLimit: number;

  constructor(error: ResultWindowLimitErrorResponse, result: SearchResult) {
    super(error);
    const { result_window_limit: resultWindowLimit } = error;

    this._state = {
      ...this._state,
      description: ResultWindowLimitError._extendDescription(result, this.description, this.queryId, this.searchTypeId, resultWindowLimit),
    };

    this._resultWindowLimit = resultWindowLimit;
  }

  static _extendDescription(result: SearchResult, description: string, queryId: QueryId, searchTypeId: SearchTypeId, resultWindowLimit: number) {
    const pageSize = ResultWindowLimitError._getPageSizeFromResult(result, queryId, searchTypeId);
    const validPages = Math.floor(resultWindowLimit / pageSize);
    const validPagesMessage = `Elasticsearch limits the search result to ${resultWindowLimit} messages. With a page size of ${pageSize} messages, you can use the first ${validPages} pages.`;

    return `${validPagesMessage} ${description}`;
  }

  static _getPageSizeFromResult(result: SearchResult, queryId: QueryId, searchTypeId: SearchTypeId) {
    const searchTypes = result.results[queryId].query.search_types;
    const searchType = searchTypes.find(({ id }) => id === searchTypeId);

    return searchType.limit;
  }

  get resultWindowLimit() { return this._resultWindowLimit; }
}
