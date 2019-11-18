import React, { forwardRef, useCallback } from 'react';
// eslint-disable-next-line no-restricted-imports
import { DropdownButton as BootstrapDropdownButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';
import buttonStyles from './styles/button';
import { propTypes, defaultProps } from './props/button';

const DropdownButton = forwardRef((props, ref) => {
  const { active, bsStyle, disabled } = props;
  const buildStyle = ({ theme }) => {
    return css`
      ${buttonStyles({ bsStyle, ...theme })};
      ${menuItemStyles({ sibling: true })};
    `;
  };

  const StyledDropdownButton = useCallback(styled(BootstrapDropdownButton)(buildStyle), [active, bsStyle, disabled]);

  return (
    <StyledDropdownButton ref={ref} {...props} />
  );
});

DropdownButton.propTypes = propTypes;

DropdownButton.defaultProps = defaultProps;


export default DropdownButton;
