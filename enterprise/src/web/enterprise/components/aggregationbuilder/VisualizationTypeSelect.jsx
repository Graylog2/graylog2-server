import React from 'react';
import PropTypes from 'prop-types';

import Select from 'components/common/Select';

const visualizationTypes = [
  { label: 'Data Table', value: 'table' },
  { label: 'Line Chart', value: 'line' },
];

const VisualizationTypeSelect = ({ onChange, value }) => {
  return (
    <Select placeholder="Visualization type" size="small" options={visualizationTypes} onChange={onChange} value={value} />
  );
};

VisualizationTypeSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
};

export default VisualizationTypeSelect;
