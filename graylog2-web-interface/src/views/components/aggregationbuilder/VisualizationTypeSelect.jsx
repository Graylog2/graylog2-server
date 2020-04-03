import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Select from 'components/common/Select';
import { defaultCompare } from 'views/logic/DefaultCompare';

const VisualizationTypeSelect = ({ onChange, value }) => {
  const visualizationTypes = PluginStore.exports('visualizationTypes')
    .sort((v1, v2) => defaultCompare(v1.displayName, v2.displayName))
    .map((viz) => ({ label: viz.displayName, value: viz.type }));

  return (
    <Select placeholder="Visualization type"
            options={visualizationTypes}
            onChange={onChange}
            clearable={false}
            value={value} />
  );
};

VisualizationTypeSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string.isRequired,
};

export default VisualizationTypeSelect;
