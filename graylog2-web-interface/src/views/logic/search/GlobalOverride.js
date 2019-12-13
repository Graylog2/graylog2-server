// @flow strict
import * as Immutable from 'immutable';
import type { QueryString, TimeRange } from '../queries/Query';

export type MessageListOptions = {
  [searchTypeId: string]: {
    limit: number;
    offset: number;
  };
}

type InternalState = {
  timerange?: TimeRange,
  query?: QueryString,
  keepSearchTypes?: string[],
  searchTypes?: MessageListOptions
};

type JsonRepresentation = {
  timerange?: TimeRange,
  query?: QueryString,
  keep_search_types?: string[],
  search_types?: MessageListOptions
};

export default class GlobalOverride {
  _value: InternalState;

  constructor(timerange?: TimeRange, query?: QueryString, keepSearchTypes?: string[], searchTypes?: MessageListOptions) {
    this._value = { timerange, query, keepSearchTypes, searchTypes };
  }

  get timerange(): ?TimeRange {
    return this._value.timerange;
  }

  get query(): ?QueryString {
    return this._value.query;
  }

  get keepSearchTypes(): ?string[] {
    return this._value.keepSearchTypes;
  }

  get searchTypes(): ?MessageListOptions {
    return this._value.searchTypes;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { timerange, query, keepSearchTypes, searchTypes } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ timerange, query, keepSearchTypes, searchTypes }));
  }

  static create(timerange?: TimeRange, query?: QueryString, keepSearchTypes?: string[], searchTypes?: MessageListOptions): GlobalOverride {
    return new GlobalOverride(timerange, query, keepSearchTypes, searchTypes);
  }

  static empty(): GlobalOverride {
    return new GlobalOverride();
  }

  toJSON(): JsonRepresentation {
    const { timerange, query, keepSearchTypes, searchTypes } = this._value;

    return {
      timerange,
      query,
      keep_search_types: keepSearchTypes,
      search_types: searchTypes,
    };
  }

  static fromJSON(value: JsonRepresentation): GlobalOverride {
    // eslint-disable-next-line camelcase
    const { timerange, query, keep_search_types, search_types } = value;
    return GlobalOverride.create(timerange, query, keep_search_types, search_types);
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: Immutable.Map = Immutable.Map()) {
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

  searchTypes(searchTypes: MessageListOptions): Builder {
    return new Builder(this.value.set('searchTypes', searchTypes));
  }

  build(): GlobalOverride {
    const { timerange, query, keepSearchTypes, searchTypes } = this.value.toObject();
    return new GlobalOverride(timerange, query, keepSearchTypes, searchTypes);
  }
}
