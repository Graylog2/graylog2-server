// @flow strict
import * as Immutable from 'immutable';
import uuid from 'uuid/v4';

import isDeepEqual from 'stores/isDeepEqual';

import type { SearchType } from './SearchType';

export type QueryId = string;

export type FilterType = Immutable.Map<string, any>;

type SearchTypeList = Array<SearchType>;
type InternalBuilderState = Immutable.Map<string, any>;

type InternalState = {
  id: QueryId,
  query: any,
  timerange: any,
  filter?: FilterType,
  searchTypes: SearchTypeList,
};

export type QueryJson = {
  id: QueryId,
  query: any,
  timerange: any,
  filter?: FilterType,
  search_types: any,
};

export type ElasticsearchQueryString = {
  type: 'elasticsearch',
  query_string: string,
};

export const createElasticsearchQueryString = (query: string = ''): ElasticsearchQueryString => ({ type: 'elasticsearch', query_string: query });

const _streamFilters = (selectedStreams: Array<string>): Array<Immutable.Map<string, string>> => {
  return selectedStreams.map((stream) => Immutable.Map({ type: 'stream', id: stream }));
};

export const filtersForQuery = (streams: ?Array<string>): ?FilterType => {
  if (!streams || streams.length === 0) {
    return null;
  }

  const streamFilters = _streamFilters(streams);

  return Immutable.Map({
    type: 'or',
    filters: streamFilters,
  });
};

export const filtersToStreamSet = (filter: ?Immutable.Map<string, any>): Immutable.Set<string> => {
  if (!filter) {
    return Immutable.Set();
  }

  const type = filter.get('type');

  if (type === 'stream') {
    return Immutable.Set([filter.get('id')]);
  }

  const filters = filter.get('filters', Immutable.List());

  return filters.map(filtersToStreamSet).reduce((prev, cur) => prev.merge(cur), Immutable.Set());
};

export type QueryString = ElasticsearchQueryString;

export type TimeRangeTypes = 'relative' | 'absolute' | 'keyword';

export type RelativeTimeRange = {|
  type: 'relative',
  range: number,
|};

export type AbsoluteTimeRange = {|
  type: 'absolute',
  from: string,
  to: string,
|};

export type KeywordTimeRange = {|
  type: 'keyword',
  keyword: string,
  from?: string,
  to?: string,
|};

export type TimeRange = RelativeTimeRange | AbsoluteTimeRange | KeywordTimeRange;

export default class Query {
  _value: InternalState;

  constructor(id: QueryId, query: any, timerange: any, filter?: FilterType, searchTypes: SearchTypeList) {
    this._value = { id, query, timerange, filter, searchTypes };
  }

  get id(): QueryId {
    return this._value.id;
  }

  get query(): QueryString {
    return this._value.query;
  }

  get timerange(): TimeRange {
    return this._value.timerange;
  }

  get filter(): ?FilterType {
    return this._value.filter;
  }

  get searchTypes(): SearchTypeList {
    return this._value.searchTypes;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, query, timerange, filter, searchTypes } = this._value;
    // eslint-disable-next-line no-use-before-define
    const builder = Query.builder()
      .id(id)
      .query(query)
      .timerange(timerange)
      .searchTypes(searchTypes);

    return filter ? builder.filter(filter) : builder;
  }

  equals(other: any): boolean {
    if (other === undefined) {
      return false;
    }

    if (!(other instanceof Query)) {
      return false;
    }

    if (this.id !== other.id
      || !isDeepEqual(this.query, other.query)
      || !isDeepEqual(this.timerange, other.timerange)
      || !isDeepEqual(this.filter, other.filter)
      || !isDeepEqual(this.searchTypes, other.searchTypes)) {
      return false;
    }

    return true;
  }

  toJSON(): QueryJson {
    const { id, query, timerange, filter, searchTypes } = this._value;

    return {
      id,
      query,
      timerange,
      filter,
      search_types: searchTypes,
    };
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .searchTypes([]);
  }

  static fromJSON(value: QueryJson): Query {
    // eslint-disable-next-line camelcase
    const { id, query, timerange, filter, search_types } = value;

    return new Query(id, query, timerange, Immutable.fromJS(filter), search_types);
  }
}

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  id(value: QueryId): Builder {
    return new Builder(this.value.set('id', value));
  }

  newId(): Builder {
    return this.id(uuid());
  }

  query(value: QueryString): Builder {
    return new Builder(this.value.set('query', value));
  }

  timerange(value: TimeRange): Builder {
    return new Builder(this.value.set('timerange', value));
  }

  filter(value: ?FilterType): Builder {
    return new Builder(this.value.set('filter', Immutable.fromJS(value)));
  }

  searchTypes(value: SearchTypeList): Builder {
    return new Builder(this.value.set('searchTypes', value));
  }

  build(): Query {
    const { id, query, timerange, filter, searchTypes } = this.value.toObject();

    return new Query(id, query, timerange, filter, searchTypes);
  }
}
