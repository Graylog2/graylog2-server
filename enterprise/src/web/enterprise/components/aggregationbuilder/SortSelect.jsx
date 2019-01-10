import React from 'react';
import PropTypes from 'prop-types';
import Select from 'react-select';
import * as Immutable from 'immutable';

import Pivot from 'enterprise/logic/aggregationbuilder/Pivot';
import Series from 'enterprise/logic/aggregationbuilder/Series';
import { defaultCompare } from 'enterprise/logic/DefaultCompare';
import PivotSortConfig from 'enterprise/logic/aggregationbuilder/PivotSortConfig';
import SeriesSortConfig from 'enterprise/logic/aggregationbuilder/SeriesSortConfig';

const mapFields = fields => fields.sort(defaultCompare)
  .map((v, idx) => ({ label: v.label, value: idx }));

const findIdxInFields = (fields, sort) => Immutable.List(fields)
  .map(field => field.value)
  .findIndex(field => sort && (field.type === sort.type && field.field === sort.field));

const mapNewValue = (fields, idx) => (fields[idx] ? [fields[idx].value] : []);

const SortSelect = ({ pivots, series, onChange, sort }) => {
  const pivotOptions = pivots.map(pivot => ({ label: pivot.field, value: PivotSortConfig.fromPivot(pivot) }));
  const seriesOptions = series.map(s => ({ label: s.effectiveName, value: SeriesSortConfig.fromSeries(s) }));
  const fields = [].concat(pivotOptions, seriesOptions);
  const value = sort && findIdxInFields(fields, sort[0]);
  const options = mapFields(fields);
  const _onChange = (newValue) => {
    const mappedValue = mapNewValue(fields, newValue);
    return onChange(mappedValue);
  };
  return (
    <Select
      placeholder="None: click to add fields"
      onChange={_onChange}
      options={options}
      simpleValue
      value={value}
    />
  );
};

SortSelect.propTypes = {
  pivots: PropTypes.arrayOf(Pivot).isRequired,
  series: PropTypes.arrayOf(Series).isRequired,
  onChange: PropTypes.func.isRequired,
  sort: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default SortSelect;
