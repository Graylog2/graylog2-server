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
// @flow strict
import URI from 'urijs';

export type AdditionalQueries = { [string]: any };

export default (destUrl: string, page: number, perPage: number, query: string, additional: AdditionalQueries = {}): string => {
  let uri = new URI(destUrl).addSearch('page', page)
    .addSearch('per_page', perPage);

  if (additional) {
    Object.keys(additional).forEach((field) => {
      const value = (additional[field] && typeof additional[field].toString === 'function')
        ? additional[field].toString()
        : additional[field];

      uri = uri.addSearch(field, value);
    });
  }

  if (query) {
    uri.addSearch('query', encodeURIComponent(query));
  }

  return uri.toString();
};
