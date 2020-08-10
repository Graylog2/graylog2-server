import React from 'react';
import styled from 'styled-components';

import Select from 'components/common/Select';

import { MS_DAY, MS_HOUR, MS_MINUTE, MS_SECOND } from './timeoutConstants';

const TimeoutSelect = styled(Select)`
  width: 150px;
`;

class TimeoutUnitSelect extends React.Component {
  options = [
    { value: `${MS_SECOND}`, label: 'Seconds' },
    { value: `${MS_MINUTE}`, label: 'Minutes' },
    { value: `${MS_HOUR}`, label: 'Hours' },
    { value: `${MS_DAY}`, label: 'Days' },
  ];

  getValue = () => {
    return this.sessionTimeoutUnit.value;
  };

  render() {
    return (
      <TimeoutSelect type="select"
                     ref={(sessionTimeoutUnit) => { this.session_timeout_unit = sessionTimeoutUnit; }}
                     options={this.options}
                     data-testid="timeout-unit-select"
                     {...this.props} />
    );
  }
}

export default TimeoutUnitSelect;
