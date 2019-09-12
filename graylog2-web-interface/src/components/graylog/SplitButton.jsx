import React from 'react';
import PropTypes from 'prop-types';
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled from 'styled-components';

import { useTheme } from 'theme/GraylogThemeContext';
import buttonStyles from './styles/buttonStyles';

const SplitButton = ({ bsStyle, ...props }) => {
  const { colors, utility } = useTheme();

  const StyledSplitButton = styled(BootstrapSplitButton)`
    ${buttonStyles({ colors, utility })};

    ~ .btn.dropdown-toggle {
      ${buttonStyles({ colors, utility, specific: false })};
    }
  `;

  return (
    <StyledSplitButton bsStyle={bsStyle} {...props} />
  );
};

SplitButton.propTypes = {
  /* NOTE: need prop so we can set default style */
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

SplitButton.defaultProps = {
  bsStyle: 'default',
};

export default SplitButton;
