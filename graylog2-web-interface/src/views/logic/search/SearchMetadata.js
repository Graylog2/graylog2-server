// @flow strict
import * as Immutable from 'immutable';
import type { QueryMetadataJson } from './QueryMetadata';
import type { ParameterJson } from '../parameters/Parameter';
import QueryMetadata from './QueryMetadata';
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
  query_metadata: { [string]: QueryMetadataJson },
  declared_parameters: { [string]: ParameterJson },
};

export default class SearchMetadata {
  _value: State;

  constructor(queryMetadata: QueryMetadataMap, declaredParameters: ParameterMap) {
    const allUsedParameterNames: Array<string> = queryMetadata.valueSeq()
      .reduce((acc: Array<string>, meta: QueryMetadata) => [...acc, ...meta.usedParameterNames.toJS()], []);
    const declaredParameterNames: Array<string> = declaredParameters.keySeq().toJS();
    const used: Array<Parameter> = Immutable.Set(allUsedParameterNames.filter(parameterName => declaredParameterNames.includes(parameterName))
      .map((parameterName: string) => declaredParameters.get(parameterName)));
    const undeclared: Array<string> = Immutable.Set(allUsedParameterNames.filter(parameterName => !declaredParameterNames.includes(parameterName)));

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
    // eslint-disable-next-line camelcase
    const { query_metadata, declared_parameters } = value;
    const queryMetadata = Immutable.Map(query_metadata)
      .map((metadata: QueryMetadataJson) => QueryMetadata.fromJSON(metadata));
    const declaredParameters = Immutable.Map(declared_parameters)
      .map((parameter: ParameterJson) => Parameter.fromJSON(parameter));
    return new SearchMetadata(queryMetadata, declaredParameters);
  }
}
