import React, { forwardRef, useCallback } from 'react';

// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import buttonStyles from './styles/button';
import menuItemStyles from './styles/menuItem';
import { propTypes, defaultProps } from './props/button';

const SplitButton = forwardRef((props, ref) => {
  const { active, bsStyle, disabled } = props;
  const buildStyle = ({ theme }) => {
    return css`
      ${buttonStyles({ bsStyle, ...theme })};

      ~ .btn.dropdown-toggle {
        ${buttonStyles({ bsStyle, ...theme })};

        ${menuItemStyles({ sibling: true })};
      }
    `;
  };

  const StyledSplitButton = useCallback(styled(BootstrapSplitButton)(buildStyle), [active, bsStyle, disabled]);

  return (
    <StyledSplitButton ref={ref} {...props} />
  );
});

SplitButton.propTypes = propTypes;

SplitButton.defaultProps = defaultProps;

export default SplitButton;
