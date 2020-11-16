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
import { mapValues } from 'lodash';

import searchTypeDefinition from 'views/logic/SearchType';

import SearchError from './SearchError';

const _findMessages = (results) => {
  return Object.keys(results.searchTypes)
    .map((id) => results.searchTypes[id])
    .find((searchType) => searchType.type.toLocaleLowerCase() === 'messages');
};

const _searchTypePlugin = (type) => {
  const typeDefinition = searchTypeDefinition(type);

  return typeDefinition && typeDefinition.handler ? searchTypeDefinition(type).handler
    : {
      convert: (result) => {
        // eslint-disable-next-line no-console
        console.log(`No search type handler for type '${type}' result:`, result);

        return result;
      },
    };
};

export default class QueryResult {
  constructor(queryResult) {
    // eslint-disable-next-line camelcase
    const { duration, timestamp, effective_timerange } = queryResult.execution_stats;

    this._state = {
      query: queryResult.query,
      errors: queryResult.errors.map((error) => new SearchError(error)),
      duration,
      timestamp,
      effectiveTimerange: effective_timerange,
      searchTypes: mapValues(queryResult.search_types, (searchType) => {
        // each search type has a custom data structure attached to it, let the plugin convert the value
        return _searchTypePlugin(searchType.type).convert(searchType);
      }),
    };
  }

  get documentCount() {
    const messages = _findMessages(this);

    return messages ? messages.total : 0;
  }

  get duration() { return this._state.duration; }

  get effectiveTimerange() { return this._state.effectiveTimerange; }

  get errors() { return this._state.errors; }

  get messages() {
    return _findMessages(this);
  }

  get query() { return this._state.query; }

  get searchTypes() { return this._state.searchTypes; }

  get timestamp() { return this._state.timestamp; }
}
