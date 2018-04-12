import React from 'react';
import PropTypes from 'prop-types';

import { FieldList } from './AggregationBuilderPropTypes';
import SortableSelect from './SortableSelect';
const RowPivotSelect = ({ fields, onChange, rowPivots }) => (
  <SortableSelect
    placeholder="Row pivot"
    onChange={onChange}
    options={fields}
    value={rowPivots}
  />
);

RowPivotSelect.propTypes = {
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
  rowPivots: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default RowPivotSelect;
