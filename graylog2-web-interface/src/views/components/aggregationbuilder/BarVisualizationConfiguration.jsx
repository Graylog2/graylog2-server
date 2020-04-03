// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import Select from 'views/components/Select';
import type { BarMode } from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import HoverForHelp from './HoverForHelp';

type Props = {
  onChange: (config: BarVisualizationConfig) => void,
  config: BarVisualizationConfig,
};

type BarModeOption = {
  label: string,
  value: BarMode,
}

class BarVisualizationConfiguration extends React.Component<Props> {
  static propTypes = {
    onChange: PropTypes.func,
    config: PropTypes.object,
  };

  static defaultProps = {
    onChange: () => {},
    config: BarVisualizationConfig.create('group'),
  };

  static options = {
    group: {
      label: 'Group',
      help: 'Every series is represented by its own bar in the chart.',
    },
    stack: {
      label: 'Stack',
      help: 'All series are stacked upon each other resulting in one bar.',
    },
    relative: {
      label: 'Relative',
      help: 'All series are stacked upon each other resulting in one chart. But negative series are placed below zero.',
    },
    overlay: {
      label: 'Overlay',
      help: 'All series are placed as bars upon each other. To be able to see the bars the opacity is reduced to 75%.'
        + ' It is recommended to use this option with not more than 3 series.',
    },
  };

  _onChange: (barmode: BarModeOption) => void = (barmode: BarModeOption) => {
    const { onChange, config } = this.props;
    const newConfig = config.toBuilder().barmode(barmode.value).build();
    onChange(newConfig);
  };

  _wrapOption: (value: BarMode) => BarModeOption = (value) => {
    const option = BarVisualizationConfiguration.options[value];
    return { label: option.label, value: value };
  };

  render = () => {
    const modes = Object.keys(BarVisualizationConfiguration.options);
    const options = modes.map(this._wrapOption);
    const { config } = this.props;

    return (
      <>
        <span>Mode:</span>
        <HoverForHelp title="Help for bar chart mode">
          <ul>
            {modes.map((mode) => (
              <li key={mode}><h4>{BarVisualizationConfiguration.options[mode].label}</h4>
                {BarVisualizationConfiguration.options[mode].help}
              </li>
            ))}
          </ul>
        </HoverForHelp>
        <Select placeholder="None: click to add series"
                onChange={this._onChange}
                options={options}
                value={this._wrapOption(config.barmode)} />
      </>
    );
  };
}

export default BarVisualizationConfiguration;
