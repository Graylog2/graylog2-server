import React from 'react';
import PropTypes from 'prop-types';

import { FieldList, PivotList } from './AggregationBuilderPropTypes';
import PivotSelect from './PivotSelect';

const RowPivotSelect = ({ fields, onChange, rowPivots }) => (
  <PivotSelect placeholder="None: click to add fields"
               onChange={onChange}
               options={fields}
               value={rowPivots} />
);

RowPivotSelect.propTypes = {
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
  rowPivots: PivotList.isRequired,
};

export default RowPivotSelect;
