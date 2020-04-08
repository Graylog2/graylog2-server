import React, { memo } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

import buttonStyles from './styles/buttonStyles';
import { propTypes, defaultProps } from './props/button';

const StyledButton = styled(BootstrapButton)(({ bsStyle, theme }) => css`
  ${buttonStyles(bsStyle, theme.color)}
`);

const Button = memo((props) => <StyledButton {...props} />);

Button.propTypes = propTypes;
Button.defaultProps = defaultProps;

export default Button;
export { StyledButton };
