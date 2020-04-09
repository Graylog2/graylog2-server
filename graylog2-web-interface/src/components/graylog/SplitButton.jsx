import React, { forwardRef, memo } from 'react';

// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';
import buttonStyles from './styles/buttonStyles';

const StyledSplitButton = styled(BootstrapSplitButton)(({ bsStyle, theme }) => css`
  ${buttonStyles(bsStyle, theme.color)};

  ~ .btn.dropdown-toggle {
    ${buttonStyles(bsStyle, theme.color)};

    & ~ {
      ${menuItemStyles}
    }
  }
`);

const SplitButton = memo(forwardRef((props, ref) => <StyledSplitButton {...props} ref={ref} />));

SplitButton.propTypes = propTypes;
SplitButton.defaultProps = defaultProps;

/** @component */
export default SplitButton;
export { StyledSplitButton };
