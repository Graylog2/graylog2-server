// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Checkbox, ControlLabel, FormGroup } from 'components/graylog';

import TypeSpecificTimeHistogramConfiguration from './TypeSpecificTimeHistogramConfiguration';

import styles from './TimeHistogramPivot.css';
import type { Interval } from './Interval';

export type IntervalConfig = {|
  interval: Interval,
|};

type Props = {
  onChange: (IntervalConfig) => void,
  value: IntervalConfig,
};

type State = IntervalConfig;

export default class TimeHistogramPivot extends React.Component<Props, State> {
  static propTypes = {
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({
      interval: PropTypes.oneOfType([
        PropTypes.shape({
          type: PropTypes.string.isRequired,
          value: PropTypes.number.isRequired,
          unit: PropTypes.string.isRequired,
        }),
        PropTypes.shape({
          type: PropTypes.string.isRequired,
          scaling: PropTypes.number,
        }),
      ]).isRequired,
    }).isRequired,
  };

  constructor(props: Props) {
    super(props);

    const { interval } = props.value;
    this.state = { interval };
  }

  _toggleAuto = () => {
    this.setState((state) => {
      if (state.interval.type === 'auto') {
        return { interval: { value: 1, unit: 'minutes', type: 'timeunit' } };
      }
      return { interval: { type: 'auto', scaling: 1.0 } };
    }, this._propagateState);
  };

  _isAuto = () => this.state && this.state.interval && this.state.interval.type === 'auto';

  _propagateState = () => this.props.onChange(this.state);

  _onChangeInterval = (interval: Interval) => this.setState((state) => ({ ...state, interval }), this._propagateState);

  render() {
    const { interval } = this.state;
    return (
      <FormGroup>
        <ControlLabel>Interval</ControlLabel>
        <Checkbox className={`pull-right ${styles.automaticCheckbox}`}
                  checked={this._isAuto()}
                  title="When this is enabled, the interval will be chosen automatically based on the timerange of the query"
                  onChange={this._toggleAuto}>
          Automatic
        </Checkbox>

        {interval && <TypeSpecificTimeHistogramConfiguration interval={interval} onChange={this._onChangeInterval} />}
      </FormGroup>
    );
  }
}
