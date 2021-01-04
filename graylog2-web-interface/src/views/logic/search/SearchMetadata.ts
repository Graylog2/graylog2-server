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

import type { QueryMetadataJson } from './QueryMetadata';
import QueryMetadata from './QueryMetadata';

import type { ParameterJson } from '../parameters/Parameter';
import Parameter from '../parameters/Parameter';

type QueryMetadataMap = Immutable.Map<string, QueryMetadata>;
type ParameterMap = Immutable.Map<string, Parameter>;

type State = {
  queryMetadata: QueryMetadataMap,
  declaredParameters: ParameterMap,
  used: Immutable.Set<Parameter>,
  undeclared: Immutable.Set<string>,
};

export type SearchMetadataJson = {
  query_metadata: { [key: string]: QueryMetadataJson },
  declared_parameters: { [key: string]: ParameterJson },
};

export default class SearchMetadata {
  _value: State;

  constructor(queryMetadata: QueryMetadataMap, declaredParameters: ParameterMap) {
    const allUsedParameterNames: Array<string> = queryMetadata.valueSeq()
      .reduce((acc: Array<string>, meta: QueryMetadata) => [...acc, ...meta.usedParameterNames.toJS()], []);
    const declaredParameterNames: Array<string> = declaredParameters.keySeq().toJS();
    const used = Immutable.Set(allUsedParameterNames.filter((parameterName) => declaredParameterNames.includes(parameterName))
      .map((parameterName: string) => declaredParameters.get(parameterName)));
    const undeclared = Immutable.Set(allUsedParameterNames.filter((parameterName) => !declaredParameterNames.includes(parameterName)));

    this._value = { queryMetadata, declaredParameters, used, undeclared };
  }

  get queryMetadata() {
    return this._value.queryMetadata;
  }

  get declaredParameters() {
    return this._value.declaredParameters;
  }

  get used(): Immutable.Set<Parameter> {
    return this._value.used;
  }

  get undeclared(): Immutable.Set<string> {
    return this._value.undeclared;
  }

  static empty(): SearchMetadata {
    return new SearchMetadata(Immutable.Map(), Immutable.Map());
  }

  static fromJSON(value: SearchMetadataJson) {
    const { query_metadata, declared_parameters } = value;
    const queryMetadata = Immutable.Map(query_metadata)
      .map((metadata: QueryMetadataJson) => QueryMetadata.fromJSON(metadata)).toMap();
    const declaredParameters = Immutable.Map(declared_parameters)
      .map((parameter: ParameterJson) => Parameter.fromJSON(parameter)).toMap();

    return new SearchMetadata(queryMetadata, declaredParameters);
  }
}
