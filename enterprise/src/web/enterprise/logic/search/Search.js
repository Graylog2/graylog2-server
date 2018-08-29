import Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import Query from '../queries/Query';

export default class Search {
  constructor(id, queries, parameters) {
    this._value = { id, queries: Immutable.fromJS(queries), parameters: Immutable.fromJS(parameters) };
  }

  static create() {
    // eslint-disable-next-line no-use-before-define
    return new Builder().newId().queries([]).parameters([])
      .build();
  }

  get id() {
    return this._value.id;
  }

  get queries() {
    return this._value.queries;
  }

  get parameters() {
    return this._value.parameters;
  }

  toBuilder() {
    const { id, queries, parameters } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, queries, parameters }));
  }

  toJSON() {
    const { id, queries, parameters } = this._value;
    return {
      id,
      queries,
      parameters,
    };
  }

  static fromJSON(value) {
    const { id, parameters } = value;

    const queries = value.queries.map(q => Query.fromJSON(q));
    return new Search(id, queries, parameters);
  }
}

class Builder {
  constructor(value = Immutable.Map()) {
    this.value = value;
  }

  id(value) {
    return new Builder(this.value.set('id', value));
  }

  newId() {
    return this.id(ObjectID().toString());
  }

  queries(value) {
    return new Builder(this.value.set('queries', value));
  }

  parameters(value) {
    return new Builder(this.value.set('parameters', value));
  }

  build() {
    const { id, queries, parameters } = this.value.toObject();
    return new Search(id, queries, parameters);
  }
}
