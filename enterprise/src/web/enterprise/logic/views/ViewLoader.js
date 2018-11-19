// @flow
import * as Immutable from 'immutable';
import { SearchParameterActions } from 'enterprise/stores/SearchParameterStore';
import { SearchExecutionStateActions } from 'enterprise/stores/SearchExecutionStateStore';

import View from './View';
import Parameter from '../parameters/Parameter';
import SearchExecutionState from '../search/SearchExecutionState';
import ParameterBinding from '../parameters/ParameterBinding';

export default function ViewLoader(view: View): Promise<View> {
  const { search } = view;
  const searchParameters: Immutable.Set<Parameter> = search.parameters || Immutable.Set();
  const parameterBindings = Immutable.Map(searchParameters.map((parameter: Parameter) => [parameter.name, ParameterBinding.forValue(parameter.defaultValue)]));
  const searchExecutionState = SearchExecutionState.create(parameterBindings);

  return Promise.all([SearchParameterActions.load(searchParameters), SearchExecutionStateActions.replace(searchExecutionState)])
    .then(() => view);
}
