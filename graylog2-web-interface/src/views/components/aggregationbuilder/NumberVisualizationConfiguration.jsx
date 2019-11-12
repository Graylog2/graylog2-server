// @flow strict
import React, { useCallback } from 'react';
import { Input } from 'components/bootstrap/index';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

type Props = {
  onChange: (config: NumberVisualizationConfig) => void,
  config: NumberVisualizationConfig,
};

const NumberVisualizationConfiguration = ({ config = NumberVisualizationConfig.create(false, false), onChange }: Props) => {
  const changeTrend = useCallback(({ target: { checked } }) => onChange(config.toBuilder().trend(checked).build()), [config, onChange]);
  const changeLowerIsBetter = useCallback(({ target: { checked } }) => onChange(config.toBuilder().lowerIsBetter(checked).build()), [config, onChange]);
  return (
    <React.Fragment>
      <Input key="trend"
             type="checkbox"
             name="trend"
             label="Display trend"
             defaultChecked={config.trend}
             onChange={changeTrend}
             help="Show trend information for this number." />

      <Input key="lowerIsBetter"
             type="checkbox"
             name="lower_is_better"
             label="Lower is better"
             disabled={config.trend === false}
             defaultChecked={config.lowerIsBetter}
             onChange={changeLowerIsBetter}
             help="Use green colour when trend goes down." />
    </React.Fragment>
  );
};

export default NumberVisualizationConfiguration;
