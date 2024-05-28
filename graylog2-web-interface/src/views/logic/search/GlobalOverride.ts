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
import type { Moment } from 'moment';
import moment from 'moment';
import trim from 'lodash/trim';

import type { QueryString, TimeRange } from '../queries/Query';

export type SearchTypeOptions<T = any> = {
  [searchTypeId: string]: T
};

type InternalState = {
  timerange?: TimeRange,
  query?: QueryString,
  keepSearchTypes?: string[],
  keepQueries?: string[],
  searchTypes?: SearchTypeOptions,
  now?: Moment | undefined | null,
};

type JsonRepresentation = {
  timerange?: TimeRange,
  query?: QueryString,
  keep_search_types?: string[],
  keep_queries?: string[],
  search_types?: SearchTypeOptions,
  now?: string,
};

function parseNow(now: string | undefined | null): Moment {
  if (now === undefined || now === null || trim(now) === '') {
    return undefined;
  }

  return moment(now);
}

export default class GlobalOverride {
  private readonly _value: InternalState;

  constructor(timerange?: TimeRange, query?: QueryString, keepSearchTypes?: string[], searchTypes?: SearchTypeOptions, keepQueries?: string[], now?: Moment) {
    this._value = { timerange, query, keepSearchTypes, searchTypes, keepQueries, now };
  }

  get timerange(): TimeRange | undefined | null {
    return this._value.timerange;
  }

  get query(): QueryString | undefined | null {
    return this._value.query;
  }

  get keepSearchTypes(): string[] | undefined | null {
    return this._value.keepSearchTypes;
  }

  get keepQueries(): string[] | undefined | null {
    return this._value.keepQueries;
  }

  get searchTypes(): SearchTypeOptions | undefined | null {
    return this._value.searchTypes;
  }

  get now(): Moment | undefined | null {
    return this._value.now;
  }

  toBuilder(): Builder {
    const { timerange, query, keepSearchTypes, searchTypes, keepQueries, now } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ timerange, query, keepSearchTypes, searchTypes, keepQueries, now }));
  }

  static create(timerange?: TimeRange, query?: QueryString, keepSearchTypes?: string[], searchTypes?: SearchTypeOptions, keepQueries?: string[], now?: Moment): GlobalOverride {
    return new GlobalOverride(timerange, query, keepSearchTypes, searchTypes, keepQueries, now);
  }

  static empty(): GlobalOverride {
    return new GlobalOverride();
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
  }

  toJSON(): JsonRepresentation {
    const { timerange, query, keepSearchTypes, keepQueries, searchTypes, now } = this._value;

    return {
      timerange,
      query,
      keep_search_types: keepSearchTypes,
      keep_queries: keepQueries,
      search_types: searchTypes,
      now: now?.toISOString(),
    };
  }

  static fromJSON(value: JsonRepresentation): GlobalOverride {
    const { timerange, query, keep_search_types, search_types, keep_queries, now } = value;

    return GlobalOverride.create(timerange, query, keep_search_types, search_types, keep_queries, parseNow(now));
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  timerange(timerange: TimeRange): Builder {
    return new Builder(this.value.set('timerange', timerange));
  }

  query(query: QueryString): Builder {
    return new Builder(this.value.set('query', query));
  }

  keepSearchTypes(keepSearchTypes: string[]): Builder {
    return new Builder(this.value.set('keepSearchTypes', keepSearchTypes));
  }

  keepQueries(keepQueries: string[]): Builder {
    return new Builder(this.value.set('keepQueries', keepQueries));
  }

  searchTypes(searchTypes: SearchTypeOptions): Builder {
    return new Builder(this.value.set('searchTypes', searchTypes));
  }

  now(now: Moment): Builder {
    return new Builder(this.value.set('now', now));
  }

  build(): GlobalOverride {
    const { timerange, query, keepSearchTypes, searchTypes, keepQueries, now } = this.value.toObject();

    return new GlobalOverride(timerange, query, keepSearchTypes, searchTypes, keepQueries, now);
  }
}
