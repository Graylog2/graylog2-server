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
export default class SearchError {
  constructor(error) {
    // eslint-disable-next-line camelcase
    const { backtrace, description, query_id, search_type_id, type } = error;

    this._state = {
      backtrace,
      description,
      query_id,
      search_type_id,
      type,
    };
  }

  get backtrace() { return this._state.backtrace; }

  get description() { return this._state.description; }

  get queryId() { return this._state.query_id; }

  get searchTypeId() { return this._state.search_type_id; }

  get type() { return this._state.type; }
}
