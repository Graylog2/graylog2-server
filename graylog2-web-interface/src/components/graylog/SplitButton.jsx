import React, { forwardRef } from 'react';

// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';
import buttonStyles from './styles/buttonStyles';

const StyledSplitButton = styled(BootstrapSplitButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)};

  ~ .btn.dropdown-toggle {
    ${buttonStyles(bsStyle)};

    & ~ {
      ${menuItemStyles}
    }
  }
`);

const SplitButton = forwardRef((props, ref) => <StyledSplitButton {...props} ref={ref} />);

SplitButton.propTypes = propTypes;
SplitButton.defaultProps = defaultProps;

/** @component */
export default SplitButton;
export { StyledSplitButton };
