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

import * as Immutable from 'immutable';
import ObjectID from 'bson-objectid';

import Query from '../queries/Query';
import Parameter from '../parameters/Parameter';
import type { ParameterJson } from '../parameters/Parameter';
import type { QueryJson } from '../queries/Query';

type SearchId = string;
type InternalState = {
  id: SearchId,
  queries: Immutable.Set<Query>,
  parameters: Immutable.Set<Parameter>,
};

export type SearchJson = {
  id: SearchId,
  queries: Array<QueryJson>,
  parameters: Array<ParameterJson>,
};

export type QuerySet = Immutable.Set<Query>;

export default class Search {
  _value: InternalState;

  constructor(id: SearchId, queries: (Array<Query> | QuerySet), parameters: Array<Parameter>) {
    this._value = { id, queries: Immutable.OrderedSet(queries), parameters: Immutable.Set(parameters) };
  }

  static create(): Search {
    // eslint-disable-next-line no-use-before-define
    return new Builder()
      .newId()
      .queries([])
      .parameters([])
      .build();
  }

  get id(): SearchId {
    return this._value.id;
  }

  get queries(): QuerySet {
    return this._value.queries;
  }

  get parameters(): Immutable.Set<Parameter> {
    return this._value.parameters;
  }

  get requiredParameters(): Immutable.Set<Parameter> {
    return this.parameters
      .filter((p) => (!p.optional && !p.defaultValue)).toSet();
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, queries, parameters } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ id, queries, parameters }));
  }

  toJSON(): SearchJson {
    const { id, queries, parameters } = this._value;

    return {
      id,
      queries: queries.toJS(),
      parameters: parameters.toJS(),
    };
  }

  static fromJSON(value: SearchJson): Search {
    const { id, parameters } = value;

    const queries = value.queries.map((q) => Query.fromJSON(q));

    return new Search(id, queries, parameters.map((p) => Parameter.fromJSON(p)));
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

class Builder {
  private value: Immutable.Map<string, any>;

  constructor(value: Immutable.Map<string, any> = Immutable.Map()) {
    this.value = value;
  }

  id(value: SearchId): Builder {
    return new Builder(this.value.set('id', value));
  }

  newId(): Builder {
    return this.id(new ObjectID().toString());
  }

  queries(value: (Array<Query> | QuerySet)): Builder {
    return new Builder(this.value.set('queries', value));
  }

  parameters(value: Array<Parameter>): Builder {
    return new Builder(this.value.set('parameters', value));
  }

  build(): Search {
    const { id, queries, parameters } = this.value.toObject();

    return new Search(id, queries, parameters);
  }
}
