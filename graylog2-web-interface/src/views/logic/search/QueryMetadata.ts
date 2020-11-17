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

type State = {
  usedParameterNames: Immutable.Set<string>,
};

export type QueryMetadataJson = {
  used_parameters_names: Array<string>,
};

export default class QueryMetadata {
  _value: State;

  constructor(usedParameterNames: Immutable.Set<string>) {
    this._value = { usedParameterNames };
  }

  get usedParameterNames(): Immutable.Set<string> {
    return this._value.usedParameterNames;
  }

  static fromJSON(value: QueryMetadataJson) {
    return new QueryMetadata(Immutable.Set(value.used_parameters_names));
  }
}
