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
import * as Immutable from 'immutable';
import URI from 'urijs';
import { $PropertyType } from 'utility-types';

import Routes from 'routing/Routes';
import type { QueryString, TimeRange } from 'views/logic/queries/Query';

import { addToQuery, escape } from '../../views/logic/queries/QueryHelper';

type InternalState = {
  id: string,
  timerange: TimeRange,
  query: QueryString,
  streams: Array<string>,
  highlightedMessage: string,
  filterFields: { [key: string]: unknown },
};

const _searchTimerange = (timerange: TimeRange) => {
  const { type } = timerange;
  const result = { rangetype: type };

  switch (timerange.type) {
    case 'relative': return { ...result, relative: timerange.range };
    case 'keyword': return { ...result, keyword: timerange.keyword };
    case 'absolute': return { ...result, from: timerange.from, to: timerange.to };
    default: return result;
  }
};

const _mergeFilterFieldsToQuery = (query: QueryString, filterFields: { [key: string]: unknown } = {}) => Object.keys(filterFields)
  .filter((key) => (filterFields[key] !== null && filterFields[key] !== undefined))
  .map((key) => `${key}:"${escape(String(filterFields[key]))}"`)
  .reduce((prev, cur) => addToQuery(prev, cur), query ? query.query_string : '');

export default class SearchLink {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    timerange: $PropertyType<InternalState, 'timerange'>,
    query: $PropertyType<InternalState, 'query'>,
    streams: $PropertyType<InternalState, 'streams'>,
    highlightedMessage: $PropertyType<InternalState, 'highlightedMessage'>,
    filterFields: $PropertyType<InternalState, 'filterFields'>,
  ) {
    this._value = {
      id,
      timerange,
      query,
      streams,
      highlightedMessage,
      filterFields,
    };
  }

  get id() {
    return this._value.id;
  }

  get timerange() {
    return this._value.timerange;
  }

  get query() {
    return this._value.query;
  }

  get streams() {
    return this._value.streams;
  }

  get highlightedMessage() {
    return this._value.highlightedMessage;
  }

  get filterFields() {
    return this._value.filterFields;
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }

  toURL() {
    const { id, query, highlightedMessage, streams, filterFields, timerange } = this._value;
    const queryWithFilterFields = _mergeFilterFieldsToQuery(query, filterFields);

    const searchTimerange = timerange ? _searchTimerange(timerange) : {};

    const params = {
      ...searchTimerange,
      q: queryWithFilterFields === '' ? undefined : queryWithFilterFields,
      highlightMessage: highlightedMessage,
    };

    const paramsWithStreams = streams && streams.length > 0
      ? { ...params, streams: streams.join(',') }
      : params;

    const urlPrefix = id ? `${Routes.SEARCH}/${id}` : Routes.SEARCH;

    const uri = new URI(urlPrefix)
      .setSearch(paramsWithStreams);

    return uri.toString();
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  id(value: $PropertyType<InternalState, 'id'>) {
    return new Builder(this.value.set('id', value));
  }

  timerange(value: $PropertyType<InternalState, 'timerange'>) {
    return new Builder(this.value.set('timerange', value));
  }

  query(value: $PropertyType<InternalState, 'query'>) {
    return new Builder(this.value.set('query', value));
  }

  streams(value: $PropertyType<InternalState, 'streams'>) {
    return new Builder(this.value.set('streams', value));
  }

  highlightedMessage(value: $PropertyType<InternalState, 'highlightedMessage'>) {
    return new Builder(this.value.set('highlightedMessage', value));
  }

  filterFields(value: $PropertyType<InternalState, 'filterFields'>) {
    return new Builder(this.value.set('filterFields', value));
  }

  build() {
    const {
      id,
      timerange,
      query,
      streams,
      highlightedMessage,
      filterFields,
    } = this.value.toObject();

    return new SearchLink(id, timerange, query, streams, highlightedMessage, filterFields);
  }
}
