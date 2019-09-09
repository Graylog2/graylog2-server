import React from 'react';
import PropTypes from 'prop-types';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled from 'styled-components';
import theme from 'styled-theming';

const backgroundColor = theme.variants('mode', 'bsStyle', {
  danger: { teinte: '#987654', noir: '#123456' },
  default: { teinte: '#987654', noir: '#123456' },
  info: { teinte: '#987654', noir: '#123456' },
  primary: { teinte: '#987654', noir: '#123456' },
  success: { teinte: '#987654', noir: '#123456' },
  warning: { teinte: '#987654', noir: '#123456' },
});

const Button = ({ bsStyle, ...props }) => {
  return (
    <StyledButton bsStyle={bsStyle} {...props} />
  );
};

Button.propTypes = {
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary']),
};

Button.defaultProps = {
  bsStyle: 'default',
};

const StyledButton = styled(BootstrapButton)`
  background-color: ${backgroundColor};
`;

export default Button;
