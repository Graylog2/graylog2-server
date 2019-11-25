// @flow strict
import React, { useCallback } from 'react';
import Select from 'react-select';
import { capitalize } from 'lodash';

import { Input } from 'components/bootstrap';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

type Props = {
  onChange: (config: NumberVisualizationConfig) => void,
  config: NumberVisualizationConfig,
};

const trendPreferenceOptions = ['LOWER', 'NEUTRAL', 'HIGHER'].map(preference => ({
  label: capitalize(preference),
  value: preference,
}));

const NumberVisualizationConfiguration = ({ config = NumberVisualizationConfig.empty(), onChange }: Props) => {
  const changeTrend = useCallback(({ target: { checked } }) => onChange(config.toBuilder().trend(checked).build()), [config, onChange]);
  const changeTrendPreference = useCallback(({ value }) => onChange(config.toBuilder().trendPreference(value).build()), [config, onChange]);
  return (
    <React.Fragment>
      <Input key="trend"
             id="trend"
             type="checkbox"
             name="trend"
             label="Display trend"
             defaultChecked={config.trend}
             onChange={changeTrend}
             help="Show trend information for this number." />

      <Input id="trend_preference" label="Trend Preference" help="Choose which trend direction is colored positively">
        <Select isDisabled={!config.trend}
                isClearable={false}
                isSearchable={false}
                options={trendPreferenceOptions}
                onChange={changeTrendPreference}
                value={{ label: config.trendPreference }} />
      </Input>
    </React.Fragment>
  );
};

export default NumberVisualizationConfiguration;
