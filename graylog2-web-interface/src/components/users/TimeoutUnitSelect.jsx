
import React from 'react';

class TimeoutUnitSelect extends React.Component {
  getValue = () => {
    return this.refs.session_timeout_unit.value;
  };

  render() {
    return (
      <select className="form-control" ref="session_timeout_unit" {...this.props}>
        <option value={1000}>Seconds</option>
        <option value={60 * 1000}>Minutes</option>
        <option value={60 * 60 * 1000}>Hours</option>
        <option value={24 * 60 * 60 * 1000}>Days</option>
      </select>
    );
  }
}

export default TimeoutUnitSelect;
