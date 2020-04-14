import React, { forwardRef } from 'react';

// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled from 'styled-components';

import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';

const StyledSplitButton = styled(BootstrapSplitButton)(({ theme }) => `
  ${theme.components.button};

  ~ .btn.dropdown-toggle {
    ${theme.components.button};

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
