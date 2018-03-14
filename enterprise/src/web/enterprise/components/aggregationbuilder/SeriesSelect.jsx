import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';
import { FieldList } from './AggregationBuilderPropTypes';

const SeriesSelect = ({ fields, onChange, series }) => (
  <Select placeholder="Series"
          size="small"
          onChange={onChange}
          options={fields}
          value={series}
          multi />
);

SeriesSelect.propTypes = {
  fields: FieldList.isRequired,
  onChange: PropTypes.func.isRequired,
  series: PropTypes.string.isRequired,
};

export default SeriesSelect;
