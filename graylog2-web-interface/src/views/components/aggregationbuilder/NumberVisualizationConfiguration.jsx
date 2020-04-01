// @flow strict
import React, { useCallback } from 'react';
import Select from 'react-select';
import { capitalize } from 'lodash';

import { Input } from 'components/bootstrap';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import HoverForHelp from './HoverForHelp';

type Props = {
  onChange: (config: NumberVisualizationConfig) => void,
  config: NumberVisualizationConfig,
};

const _makeOption = (value) => ({ label: capitalize(value), value });
const trendPreferenceOptions = ['LOWER', 'NEUTRAL', 'HIGHER'].map(_makeOption);

const NumberVisualizationConfiguration = ({ config = NumberVisualizationConfig.empty(), onChange }: Props) => {
  const changeTrend = useCallback(({ target: { checked } }) => onChange(config.toBuilder().trend(checked).build()), [config, onChange]);
  const changeTrendPreference = useCallback(({ value }) => onChange(config.toBuilder().trendPreference(value).build()), [config, onChange]);
  const trendingHelp = (
    <HoverForHelp title="Trending">
      <p>
        If the user enables trending, a separate box is shown below the current value, indicating the direction of the change
        by an icon as well as the absolute and the relative differences between the current value and the previous one.
      </p>

      <p>
        The previous value is calculated by performing two searches in the background, which are completely identical besides
        the timerange. The timerange of the first search is identical to the one configured for this query/this widget,
        the second one is the same timerange, but with an offset of the timerange length shifted to the past.
      </p>
    </HoverForHelp>
  );
  return (
    <>
      <Input key="trend"
             id="trend"
             type="checkbox"
             name="trend"
             label={<span>Display trend {trendingHelp}</span>}
             defaultChecked={config.trend}
             onChange={changeTrend}
             help="Show trend information for this number." />

      <Input id="trend_preference" label="Trend Preference" help="Choose which trend direction is colored positively">
        <Select isDisabled={!config.trend}
                isClearable={false}
                isSearchable={false}
                options={trendPreferenceOptions}
                onChange={changeTrendPreference}
                value={_makeOption(config.trendPreference)} />
      </Input>
    </>
  );
};

export default NumberVisualizationConfiguration;
