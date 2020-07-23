import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { buttonStyles } from './Button';
import menuItemStyles from './styles/menuItem';

const SplitButton = React.memo(styled(BootstrapSplitButton)(({ bsStyle }) => css`
  ${buttonStyles(bsStyle)}
  ~ .btn.dropdown-toggle {
    ${buttonStyles(bsStyle)}
    & ~ {
      ${menuItemStyles}
    }
  }
`));

/** @component */
export default SplitButton;
