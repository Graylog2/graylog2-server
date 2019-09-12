import React from 'react';
import PropTypes from 'prop-types';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled from 'styled-components';

import { useTheme } from 'theme/GraylogThemeContext';
import buttonStyles from './styles/buttonStyles';

const Button = ({ bsStyle, ...props }) => {
  const { colors, utility } = useTheme();

  const StyledButton = styled(BootstrapButton)`
    ${buttonStyles({ colors, utility })};
  `;

  return (
    <StyledButton bsStyle={bsStyle} {...props} />
  );
};

Button.propTypes = {
  /* NOTE: need prop so we can set default style */
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

Button.defaultProps = {
  bsStyle: 'default',
};

export default Button;
