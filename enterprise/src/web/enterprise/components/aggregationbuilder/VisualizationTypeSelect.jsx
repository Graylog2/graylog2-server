import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import naturalSort from 'javascript-natural-sort';

import Select from 'components/common/Select';

const VisualizationTypeSelect = ({ onChange, value }) => {
  const visualizationTypes = PluginStore.exports('visualizationTypes')
    .map(viz => ({ label: viz.displayName, value: viz.type }))
    .sort((v1, v2) => naturalSort(v1.displayName, v2.displayName));

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
