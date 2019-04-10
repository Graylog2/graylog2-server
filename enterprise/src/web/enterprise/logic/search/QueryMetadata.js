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
