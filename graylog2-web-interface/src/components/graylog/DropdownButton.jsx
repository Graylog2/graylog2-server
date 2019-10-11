import React from 'react';
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const DropdownButton = ({ active, bsStyle, ...props }) => {
  const StyledDropdownButton = styled(BootstrapDropdownButton)`
    ${buttonStyles({ active })};
  `;

  return (
    <StyledDropdownButton bsStyle={bsStyle} {...props} />
  );
};

DropdownButton.propTypes = propTypes;

DropdownButton.defaultProps = defaultProps;

export default DropdownButton;
