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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Icon } from 'components/common';

const SidecarIcon = styled(Icon)`
  margin-right: 5px;
  margin-left: 2px;
`;

const OperatingSystemIcon = ({ operatingSystem }) => {
  let iconName = 'question-circle';
  let iconType = 'solid';

  if (operatingSystem) {
    const os = operatingSystem.trim().toLowerCase();

    if (os.indexOf('darwin') !== -1 || os.indexOf('mac os') !== -1) {
      iconName = 'apple';
      iconType = 'brand';
    } else if (os.indexOf('linux') !== -1) {
      iconName = 'linux';
      iconType = 'brand';
    } else if (os.indexOf('win') !== -1) {
      iconName = 'windows';
      iconType = 'brand';
    }
  }

  return (
    <SidecarIcon name={iconName} type={iconType} fixedWidth />
  );
};

OperatingSystemIcon.propTypes = {
  operatingSystem: PropTypes.string,
};

OperatingSystemIcon.defaultProps = {
  operatingSystem: undefined,
};

export default OperatingSystemIcon;
