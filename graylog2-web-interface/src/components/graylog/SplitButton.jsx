import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';

const SplitButton = styled(BootstrapSplitButton)(({ theme }) => css`
  ${theme.components.button}
  ~ .btn.dropdown-toggle {
    ${theme.components.button}
    & ~ {
      ${menuItemStyles}
    }
  }
`);

/** @component */
export default SplitButton;
