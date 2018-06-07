import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import naturalSort from 'javascript-natural-sort';

import Select from 'components/common/Select';

const VisualizationTypeSelect = ({ onChange, value }) => {
  const visualizationTypes = PluginStore.exports('visualizationTypes')
    .sort((v1, v2) => naturalSort(v1.displayName, v2.displayName))
    .map(viz => ({ label: viz.displayName, value: viz.type }));

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
