import React from 'react';
import PropTypes from 'prop-types';

import SortableSelect from './SortableSelect';
import { FieldList, PivotList } from './AggregationBuilderPropTypes';

const ColumnPivotSelect = ({ columnPivots, fields, onChange }) => (
  <SortableSelect
    placeholder="None: click to add fields"
    onChange={onChange}
    options={fields}
    value={columnPivots}
  />
);

ColumnPivotSelect.propTypes = {
  columnPivots: PivotList.isRequired,
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default ColumnPivotSelect;
