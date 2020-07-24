import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';

const DropdownButton = styled(BootstrapDropdownButton)(({ theme }) => css`
  ${theme.components.button}
  & ~ {
    ${menuItemStyles}
  }
`);

/** @component */
export default DropdownButton;
