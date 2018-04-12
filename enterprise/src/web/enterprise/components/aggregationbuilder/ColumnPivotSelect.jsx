import React from 'react';
import PropTypes from 'prop-types';

import { FieldList } from './AggregationBuilderPropTypes';
import SortableSelect from './SortableSelect';

const ColumnPivotSelect = ({ columnPivots, fields, onChange }) => (
  <SortableSelect
    placeholder="Column pivot"
    onChange={onChange}
    options={fields}
    value={columnPivots}
  />
);

ColumnPivotSelect.propTypes = {
  columnPivots: PropTypes.arrayOf(PropTypes.string).isRequired,
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default ColumnPivotSelect;
