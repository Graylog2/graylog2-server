import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { FieldList } from './AggregationBuilderPropTypes';

const ColumnPivotSelect = ({ columnPivots, fields, onChange }) => (
  <Select placeholder="Column pivot"
          options={fields}
          multi
          value={columnPivots}
          onChange={onChange} />
);

ColumnPivotSelect.propTypes = {
  columnPivots: PropTypes.arrayOf(PropTypes.string).isRequired,
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
};

export default ColumnPivotSelect;
