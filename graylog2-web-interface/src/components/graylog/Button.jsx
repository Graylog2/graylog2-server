import React from 'react';
import PropTypes from 'prop-types';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled from 'styled-components';

import { useTheme } from 'theme/GraylogThemeContext';
import buttonStyles from './styles/buttonStyles';

const Button = ({ active, bsStyle, ...props }) => {
  const { colors, utility } = useTheme();

  const StyledButton = styled(BootstrapButton)`
    ${buttonStyles({ colors, active, utility })};
  `;

  return (
    <StyledButton active={active} bsStyle={bsStyle} {...props} />
  );
};

Button.propTypes = {
  /* NOTE: need props so we can set default styles */
  active: PropTypes.bool,
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

Button.defaultProps = {
  active: false,
  bsStyle: 'default',
};

export default Button;
