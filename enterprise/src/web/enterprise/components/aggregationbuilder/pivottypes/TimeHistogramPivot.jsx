import React from 'react';
import PropTypes from 'prop-types';
import {
  Checkbox,
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

  _toggleAuto = () => {
    this.setState((state) => {
      if (state.type === 'auto') {
        return { value: 1, unit: 'minutes', type: 'timeunit' };
      }
      return { type: 'auto', value: undefined, unit: undefined };
    }, this._propagateState);
  };

  _isAuto = () => this.state.type === 'auto';
  _propagateState = () => this.props.onChange({ interval: this.state });

  _changeUnit = unit => this.setState({ unit }, this._propagateState);
  _changeValue = event => this.setState({ value: FormsUtils.getValueFromInput(event.target) }, this._propagateState);

  render() {
    const units = TimeHistogramPivot.units;
    return (
      <FormGroup>
        <ControlLabel>Interval</ControlLabel>
        <InputGroup>
          <FormControl type="number" value={this._isAuto() ? '' : this.state.value} onChange={this._changeValue} disabled={this._isAuto()} />
          <DropdownButton
            componentClass={InputGroup.Button}
            id="input-dropdown-addon"
            title={units[this.state.unit]}
            disabled={this._isAuto()}
            onChange={this._changeUnit}
          >
            {Object.keys(units).map(unit => <MenuItem key={unit} onSelect={() => this._changeUnit(unit)}>{units[unit]}</MenuItem>)}
          </DropdownButton>
        </InputGroup>
        <Checkbox checked={this._isAuto()} onChange={this._toggleAuto}>
          Choose automatically
        </Checkbox>
        <HelpBlock>The size of the buckets for this timestamp type</HelpBlock>
      </FormGroup>
    );
  }
};
