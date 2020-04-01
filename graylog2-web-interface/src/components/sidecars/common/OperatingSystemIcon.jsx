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
  let prefix = 'fas';

  if (operatingSystem) {
    const os = operatingSystem.trim().toLowerCase();
    if (os.indexOf('darwin') !== -1 || os.indexOf('mac os') !== -1) {
      iconName = 'apple';
      prefix = 'fab';
    } else if (os.indexOf('linux') !== -1) {
      iconName = 'linux';
      prefix = 'fab';
    } else if (os.indexOf('win') !== -1) {
      iconName = 'windows';
      prefix = 'fab';
    }
  }

  return (
    <SidecarIcon name={{ prefix, iconName }} fixedWidth />
  );
};

OperatingSystemIcon.propTypes = {
  operatingSystem: PropTypes.string,
};

OperatingSystemIcon.defaultProps = {
  operatingSystem: undefined,
};

export default OperatingSystemIcon;
