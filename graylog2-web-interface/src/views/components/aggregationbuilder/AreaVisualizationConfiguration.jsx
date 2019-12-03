// @flow strict
import React, { useCallback } from 'react';
import { capitalize } from 'lodash';

import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import Select from '../Select';

type Props = {
  onChange: (config: AreaVisualizationConfig) => void,
  config: AreaVisualizationConfig,
};

const _makeOption = value => ({ label: capitalize(value), value });
const interpolationOptions = ['linear', 'step-after', 'spline'].map(_makeOption);

const AreaVisualizationConfiguration = ({ config = AreaVisualizationConfig.empty(), onChange }: Props) => {
  const _onChange = useCallback(({ value }) => onChange(config.toBuilder().interpolation(value).build()), [config, onChange]);
  return (
    <React.Fragment>
      <span>Interpolation:</span>
      <Select placeholder="Select Interpolation Mode"
              onChange={_onChange}
              options={interpolationOptions}
              value={_makeOption(config.interpolation)} />
    </React.Fragment>
  );
};

export default AreaVisualizationConfiguration;
