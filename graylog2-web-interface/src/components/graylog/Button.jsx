import React, { forwardRef, useMemo } from 'react';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Button as BootstrapButton } from 'react-bootstrap';

import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const Button = forwardRef((props, ref) => {
  const { active, bsStyle, disabled } = props;
  const StyledButton = useMemo(
    () => styled(BootstrapButton)`${buttonStyles(props)}`,
    [active, bsStyle, disabled],
  );

  return (
    <StyledButton ref={ref} {...props} />
  );
});

Button.propTypes = propTypes;

Button.defaultProps = defaultProps;

export default Button;
