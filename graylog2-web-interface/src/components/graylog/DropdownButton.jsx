import React from 'react';
import PropTypes from 'prop-types';
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled from 'styled-components';

import { useTheme } from 'theme/GraylogThemeContext';
import buttonStyles from './styles/buttonStyles';

const DropdownButton = ({ active, bsStyle, ...props }) => {
  const { colors, utility } = useTheme();

  const StyledDropdownButton = styled(BootstrapDropdownButton)`
    ${buttonStyles({ colors, active, utility })};
  `;

  return (
    <StyledDropdownButton bsStyle={bsStyle} {...props} />
  );
};

DropdownButton.propTypes = {
  /* NOTE: need prop so we can set default style */
  active: PropTypes.bool,
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

DropdownButton.defaultProps = {
  active: false,
  bsStyle: 'default',
};

export default DropdownButton;
