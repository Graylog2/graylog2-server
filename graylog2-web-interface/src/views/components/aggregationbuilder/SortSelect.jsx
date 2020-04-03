// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import * as Immutable from 'immutable';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import { defaultCompare } from 'views/logic/DefaultCompare';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import { PivotList, SeriesList, SortList } from './AggregationBuilderPropTypes';

const mapFields = (fields) => fields.sort(defaultCompare)
  .map((v, idx) => ({ label: v.label, value: idx }));

const findIdxInFields = (fields, sort) => Immutable.List(fields)
  .map((field) => field.value)
  .findIndex((field) => sort && (field.type === sort.type && field.field === sort.field));

const mapNewValue = (fields, idx) => (fields[idx] ? [fields[idx].value] : []);

type Props = {
  pivots: Array<Pivot>,
  series: Array<Series>,
  onChange: (Array<*>) => any,
  sort: Array<SortConfig>,
};

const currentValue = (sort, fields) => {
  const currentIdx = sort && findIdxInFields(fields, sort[0]);
  if (currentIdx === undefined) {
    return undefined;
  }
  return fields[currentIdx];
};

const SortSelect = ({ pivots, series, onChange, sort }: Props) => {
  const pivotOptions = pivots.map((pivot) => ({ label: pivot.field, value: SortConfig.fromPivot(pivot) }));
  const seriesOptions = series.map((s) => ({ label: s.effectiveName, value: SortConfig.fromSeries(s) }));
  const fields = [].concat(pivotOptions, seriesOptions);
  const options = mapFields(fields);
  const _onChange = (newValue, reason) => {
    if (reason.action === 'clear') {
      return onChange([]);
    }
    const { value } = newValue;
    const mappedValue = mapNewValue(fields, value);
    return onChange(mappedValue);
  };
  return (
    <Select placeholder="None: click to add fields"
            onChange={_onChange}
            options={options}
            isClearable
            value={currentValue(sort, fields)} />
  );
};

SortSelect.propTypes = {
  pivots: PivotList.isRequired,
  series: SeriesList.isRequired,
  onChange: PropTypes.func.isRequired,
  sort: SortList.isRequired,
};

export default SortSelect;
