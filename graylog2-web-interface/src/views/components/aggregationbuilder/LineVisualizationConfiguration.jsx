// @flow strict
import React, { useCallback } from 'react';
import { capitalize } from 'lodash';

import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import Select from '../Select';

type Props = {
  onChange: (config: LineVisualizationConfig) => void,
  config: LineVisualizationConfig,
};

const _makeOption = value => ({ label: capitalize(value), value });
const interpolationOptions = ['linear', 'step-after', 'spline'].map(_makeOption);

const LineVisualizationConfiguration = ({ config = LineVisualizationConfig.empty(), onChange }: Props) => {
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

export default LineVisualizationConfiguration;
