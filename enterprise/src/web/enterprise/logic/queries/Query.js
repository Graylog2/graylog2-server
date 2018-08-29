import Immutable, { is } from 'immutable';

export default class Query {
  constructor(id, query, timerange, filter, searchTypes) {
    this._value = { id, query, timerange, filter, searchTypes };
  }

  get id() {
    return this._value.id;
  }

  get query() {
    return this._value.query;
  }

  get timerange() {
    return this._value.timerange;
  }

  get filter() {
    return this._value.filter;
  }

  get searchTypes() {
    return this._value.searchTypes;
  }

  toBuilder() {
    const { id, query, timerange, filter, searchTypes } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, query, timerange, filter, searchTypes }));
  }

  equals(other) {
    if (other === undefined) {
      return false;
    }
    if (!(other instanceof Query)) {
      return false;
    }

    if (this.id !== other.id || !is(this.query, other.query) || !is(this.timerange, other.timerange) || !is(this.filter, other.filter) || !is(this.searchTypes, other.searchTypes)) {
      return false;
    }

    return true;
  }

  toJSON() {
    const { id, query, timerange, filter, searchTypes } = this._value;

    return {
      id,
      query,
      timerange,
      filter,
      search_types: searchTypes,
    };
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { id, query, timerange, filter, search_types } = value;
    return new Query(id, query, timerange, Immutable.fromJS(filter), search_types);
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  id(value) {
    return new Builder(this.value.set('id', value));
  }

  query(value) {
    return new Builder(this.value.set('query', value));
  }

  timerange(value) {
    return new Builder(this.value.set('timerange', value));
  }

  filter(value) {
    return new Builder(this.value.set('filter', Immutable.fromJS(value)));
  }

  searchTypes(value) {
    return new Builder(this.value.set('searchTypes', value));
  }

  build() {
    const { id, query, timerange, filter, searchTypes } = this.value.toObject();
    return new Query(id, query, timerange, filter, searchTypes);
  }
}
