import { flatten } from 'lodash';

import AggregationFunctionsStore from 'views/stores/AggregationFunctionsStore';
import Series from 'views/logic/aggregationbuilder/Series';

import { parameterNeededForType } from './SeriesParameterOptions';

const _makeIncompleteFunction = (fun) => ({ label: `${fun}(...)`, value: fun, incomplete: true, parameterNeeded: parameterNeededForType(fun) });

const _wrapOption = (series) => ({ label: series.effectiveName, value: series });

const _defaultFunctions = (functions) => {
  const funcOptions = Object.keys(functions).map(_makeIncompleteFunction);
  return [].concat(
    [_wrapOption(Series.forFunction('count()'))],
    funcOptions,
  );
};

const combineFunctionsWithFields = (functions, fields, parameter) => flatten(fields.map((name) => functions.map((f) => {
  if (parameter) {
    return `${f}(${name},${parameter})`;
  }
  return `${f}(${name})`;
})));

export default class SeriesFunctionsSuggester {
  constructor(fields) {
    this.fields = fields;
    this._updateFunctions(AggregationFunctionsStore.getInitialState());
    AggregationFunctionsStore.listen(this._updateFunctions);
  }

  _updateFunctions = (functions) => {
    if (functions) {
      this.functions = functions;
      this.defaultFunctions = _defaultFunctions(functions);
    }
  };

  get defaults() {
    return this.defaultFunctions;
  }

  for = (func, parameter) => combineFunctionsWithFields([func], this.fields, parameter).map(Series.forFunction).map(_wrapOption);
}
