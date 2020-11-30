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
