import React from 'react';
import PropTypes from 'prop-types';

import SortableSelect from './SortableSelect';
import { FieldList, PivotList } from './AggregationBuilderPropTypes';

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
  rowPivots: PivotList.isRequired,
};

export default RowPivotSelect;
