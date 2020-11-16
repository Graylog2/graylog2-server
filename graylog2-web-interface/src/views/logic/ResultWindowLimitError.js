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
import SearchError from './SearchError';

export default class ResultWindowLimitError extends SearchError {
  constructor(error, result) {
    super(error);
    const { result_window_limit: resultWindowLimit } = error;

    this._state = {
      ...this._state,
      description: ResultWindowLimitError._extendDescription(result, this.description, this.queryId, this.searchTypeId, resultWindowLimit),
      result_window_limit: resultWindowLimit,
    };
  }

  static _extendDescription(result, description, queryId, searchTypeId, resultWindowLimit) {
    const pageSize = ResultWindowLimitError._getPageSizeFromResult(result, queryId, searchTypeId);
    const validPages = Math.floor(resultWindowLimit / pageSize);
    const validPagesMessage = `Elasticsearch limits the search result to ${resultWindowLimit} messages. With a page size of ${pageSize} messages, you can use the first ${validPages} pages.`;

    return `${validPagesMessage} ${description}`;
  }

  static _getPageSizeFromResult(result, queryId, searchTypeId) {
    const searchTypes = result.results[queryId].query.search_types;
    const searchType = searchTypes.find(({ id }) => id === searchTypeId);

    return searchType.limit;
  }

  get resultWindowLimit() { return this._state.result_window_limit; }
}
