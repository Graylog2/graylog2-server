import React, { useCallback } from 'react';
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled from 'styled-components';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const SplitButton = ({ active, bsStyle, ...props }) => {
  const StyledSplitButton = useCallback(styled(BootstrapSplitButton)`
    ${buttonStyles({ active })};

    ~ .btn.dropdown-toggle {
      ${buttonStyles({ active, specific: false })};
    }
  `);

  return (
    <StyledSplitButton bsStyle={bsStyle} {...props} />
  );
};

SplitButton.propTypes = propTypes;

SplitButton.defaultProps = defaultProps;

export default SplitButton;
