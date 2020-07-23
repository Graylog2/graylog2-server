import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { buttonStyles } from './Button';
import menuItemStyles from './styles/menuItem';

const StyledSplitButton = styled(BootstrapSplitButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)}
  ~ .btn.dropdown-toggle {
    ${buttonStyles(bsStyle)}
    & ~ {
      ${menuItemStyles}
    }
  }
`);

const SplitButton = React.memo((props) => <StyledSplitButton {...props} />);

/** @component */
export default SplitButton;
