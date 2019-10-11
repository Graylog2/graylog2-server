import React, { forwardRef, useCallback } from 'react';
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const SplitButton = forwardRef(({ active, bsStyle, ...props }, ref) => {
  const StyledSplitButton = useCallback(styled(BootstrapSplitButton)`
    ${buttonStyles({ active })};

    ~ .btn.dropdown-toggle {
      ${buttonStyles({ active, specific: false })};
    }
  `, [active]);

  return (
    <StyledSplitButton bsStyle={bsStyle} ref={ref} {...props} />
  );
});

SplitButton.propTypes = propTypes;

SplitButton.defaultProps = defaultProps;

export default SplitButton;
