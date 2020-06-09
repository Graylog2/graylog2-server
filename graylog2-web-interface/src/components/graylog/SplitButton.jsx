import React, { forwardRef } from 'react';

// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';

const StyledSplitButton = styled(BootstrapSplitButton)(({ theme }) => css`
  ${theme.components.button};

  ~ .btn.dropdown-toggle {
    ${theme.components.button};

    & ~ {
      ${menuItemStyles}
    }
  }
`);

const SplitButton = forwardRef((props, ref) => <StyledSplitButton {...props} ref={ref} />);

/** @component */
export default SplitButton;
export { StyledSplitButton };
