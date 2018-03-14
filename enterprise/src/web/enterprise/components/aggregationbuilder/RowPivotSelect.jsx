import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { FieldList } from './AggregationBuilderPropTypes';

const RowPivotSelect = ({ fields, onChange, rowPivots }) => (
  <Select placeholder="Row pivot" size="small" options={fields} multi value={rowPivots} onChange={onChange} />
);

RowPivotSelect.propTypes = {
  rowPivots: PropTypes.arrayOf(PropTypes.string).isRequired,
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default RowPivotSelect;
