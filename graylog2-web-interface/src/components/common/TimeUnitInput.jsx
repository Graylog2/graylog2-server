import React, { PropTypes } from 'react';
import {
  InputGroup, FormGroup, ControlLabel, FormControl, HelpBlock, DropdownButton, MenuItem
} from 'react-bootstrap';
import { InputWrapper } from 'components/bootstrap';

const TimeUnitInput = React.createClass({
  propTypes: {
    update: PropTypes.func.isRequired,
    label: PropTypes.string,
    help: PropTypes.string,
    required: PropTypes.bool,
    enabled: PropTypes.bool,
    value: PropTypes.number,
    unit: PropTypes.oneOf(['NANOSECONDS', 'MICROSECONDS', 'MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']),
    labelClassName: PropTypes.string,
    wrapperClassName: PropTypes.string,
  },

  getDefaultProps() {
    return {
      value: 1,
      unit: 'SECONDS',
      label: '',
      help: '',
      required: false,
      enabled: false,
      labelClassName: undefined,
      wrapperClassName: undefined,
    };
  },

  getInitialState() {
    return {
      checked: this.props.required || this.props.enabled,
      value: this.props.value,
      unit: this.props.unit,
    };
  },

  OPTIONS: [
    { value: 'NANOSECONDS', label: 'nanoseconds' },
    { value: 'MICROSECONDS', label: 'microseconds' },
    { value: 'MILLISECONDS', label: 'milliseconds' },
    { value: 'SECONDS', label: 'seconds' },
    { value: 'MINUTES', label: 'minutes' },
    { value: 'HOURS', label: 'hours' },
    { value: 'DAYS', label: 'days' },
  ],

  _propagateState() {
    this.props.update(this.state.value, this.state.unit, this.state.checked);
  },

  _onToggleEnable(e) {
    this.setState({ checked: e.target.checked }, this._propagateState);
  },

  _onUpdate(e) {
    const value = e.target.value;
    this.setState({ value: value }, this._propagateState);
  },

  _onUnitSelect(unit) {
    this.setState({ unit: unit }, this._propagateState);
  },

  render() {
    const options = this.OPTIONS.map((o) => {
      return <MenuItem key={o.value} onSelect={() => this._onUnitSelect(o.value)}>{o.label}</MenuItem>;
    });

    const checkbox = (<InputGroup.Addon>
      <input type="checkbox" checked={this.state.checked} onChange={this._onToggleEnable} />
    </InputGroup.Addon>);

    return (
      <FormGroup>
        {this.props.label && <ControlLabel className={this.props.labelClassName}>{this.props.label}</ControlLabel>}
        <InputWrapper className={this.props.wrapperClassName}>
          <InputGroup>
            {!this.props.required && checkbox}
            <FormControl type="text" disabled={!this.state.checked} onChange={this._onUpdate} value={this.state.value} />
            <DropdownButton componentClass={InputGroup.Button}
                            id="input-dropdown-addon"
                            title={this.OPTIONS.filter(o => o.value === this.state.unit)[0].label}>
              {options}
            </DropdownButton>
          </InputGroup>
          {this.props.help && <HelpBlock>{this.props.help}</HelpBlock>}
        </InputWrapper>
      </FormGroup>
    );
  },
});

export default TimeUnitInput;
