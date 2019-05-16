import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment/moment';
import { Select } from 'components/common';

const durationValue = duration => moment.duration(duration).asMilliseconds() / 1000;

const timerangeOptions = [
  { value: durationValue('PT5M'), label: 'Search in the last 5 minutes' },
  { value: durationValue('PT15M'), label: 'Search in the last 15 minutes' },
  { value: durationValue('PT30M'), label: 'Search in the last 30 minutes' },
  { value: durationValue('PT1H'), label: 'Search in the last 1 hour' },
  { value: durationValue('PT2H'), label: 'Search in the last 2 hours' },
  { value: durationValue('PT8H'), label: 'Search in the last 8 hours' },
  { value: durationValue('P1D'), label: 'Search in the last 1 day' },
  { value: durationValue('P2D'), label: 'Search in the last 2 days' },
  { value: durationValue('P5D'), label: 'Search in the last 5 days' },
  { value: durationValue('P7D'), label: 'Search in the last 7 days' },
  { value: durationValue('P14D'), label: 'Search in the last 14 days' },
  { value: durationValue('P30D'), label: 'Search in the last 30 days' },
  { value: durationValue('PT0S'), label: 'Search in all messages' },
];

export default class SimpleTimerangeSelector extends React.Component {
  static propTypes = {
    value: PropTypes.number,
    onChange: PropTypes.func.isRequired,
  };

  static defaultProps = {
    value: timerangeOptions[0].value,
  };

  handleChange = (newValue) => {
    this.props.onChange(newValue);
  };

  render() {
    return (
      <Select placeholder="Select a timerange"
              options={timerangeOptions}
              onChange={this.handleChange}
              value={this.props.value} />
    );
  }
}
