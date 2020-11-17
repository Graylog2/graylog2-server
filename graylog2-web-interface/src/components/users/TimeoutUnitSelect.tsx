/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import styled from 'styled-components';

import { Select } from 'components/common';

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
      <TimeoutSelect ref={(sessionTimeoutUnit) => { this.session_timeout_unit = sessionTimeoutUnit; }}
                     options={this.options}
                     {...this.props} />
    );
  }
}

export default TimeoutUnitSelect;
