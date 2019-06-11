import React from 'react';
import PropTypes from 'prop-types';

import { FieldList, PivotList } from './AggregationBuilderPropTypes';
import PivotSelect from './PivotSelect';

const ColumnPivotSelect = ({ columnPivots, fields, onChange }) => (
  <PivotSelect placeholder="None: click to add fields"
               onChange={onChange}
               options={fields}
               value={columnPivots} />
);

ColumnPivotSelect.propTypes = {
  columnPivots: PivotList.isRequired,
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default ColumnPivotSelect;
