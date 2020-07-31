import React from 'react';

class TimeoutUnitSelect extends React.Component {
  getValue = () => {
    return this.sessionTimeoutUnit.value;
  };

  render() {
    return (
      <select className="form-control"
              ref={(sessionTimeoutUnit) => { this.sessionTimeoutUnit = sessionTimeoutUnit; }}
              data-testid="timeout-unit-select"
              {...this.props}>
        <option value={1000}>Seconds</option>
        <option value={60 * 1000}>Minutes</option>
        <option value={60 * 60 * 1000}>Hours</option>
        <option value={24 * 60 * 60 * 1000}>Days</option>
      </select>
    );
  }
}

export default TimeoutUnitSelect;
