import React from 'react';
import PropTypes from 'prop-types';
import {
  ControlLabel,
  DropdownButton,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  MenuItem,
} from 'react-bootstrap';

import FormsUtils from 'util/FormsUtils';

export default class TimeHistogramPivot extends React.Component {
  static propTypes = {
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({
      value: PropTypes.number.isRequired,
      unit: PropTypes.string.isRequired,
    }).isRequired,
  };

  static units = {
    seconds: 'Seconds',
    minutes: 'Minutes',
    hours: 'Hours',
    days: 'Days',
    weeks: 'Weeks',
    months: 'Months',
  };

  constructor(props, context) {
    super(props, context);

    const { interval } = props.value;
    this.state = interval;
  }

  _changeUnit = unit => this.setState({ unit }, () => this.props.onChange({ interval: this.state }));
  _changeValue = event => this.setState({ value: FormsUtils.getValueFromInput(event.target) }, () => this.props.onChange({ interval: this.state }));

  render() {
    const units = TimeHistogramPivot.units;
    return (
      <FormGroup>
        <ControlLabel>Interval</ControlLabel>
        <InputGroup>
          <FormControl type="text" value={this.state.value} onChange={this._changeValue} />
          <DropdownButton
            componentClass={InputGroup.Button}
            id="input-dropdown-addon"
            title={units[this.state.unit]}
            onChange={this._changeUnit}
          >
            {Object.keys(units).map(unit => <MenuItem key={unit} onSelect={() => this._changeUnit(unit)}>{units[unit]}</MenuItem>)}
          </DropdownButton>
        </InputGroup>
        <HelpBlock>The size of the buckets for this timestamp type</HelpBlock>
      </FormGroup>
    );
  }
};
