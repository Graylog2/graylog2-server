import { flatten } from 'lodash';

import AggregationFunctionsStore from 'enterprise/stores/AggregationFunctionsStore';
import Series from 'enterprise/logic/aggregationbuilder/Series';

const _makeIncompleteFunction = fun => ({ label: `${fun}(...)`, value: fun, incomplete: true });

const _wrapOption = series => ({ label: series.effectiveName, value: series });

const _defaultFunctions = (functions) => {
  return [].concat(
    [_wrapOption(Series.forFunction('count()'))],
    Object.keys(functions).map(_makeIncompleteFunction),
  );
};

const combineFunctionsWithFields = (functions, fields) => flatten(fields.map(name => functions.map(f => `${f}(${name})`)));

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

  for = func => combineFunctionsWithFields([func], this.fields).map(Series.forFunction).map(_wrapOption);
}
