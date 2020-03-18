import React, { forwardRef, memo } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const StyledButton = memo(styled(BootstrapButton)(({ theme }) => css`
  ${buttonStyles(theme.color)};
`));

const Button = forwardRef((props, ref) => {
  return (
    <StyledButton ref={ref} {...props} />
  );
});

Button.propTypes = propTypes;

Button.defaultProps = defaultProps;

export default Button;
