import React from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';

type Props = {
  onClick: () => void,
  isOpen: boolean
}
const StyledButton = styled(Button)(({ theme }) => css`
  border: 0;
  font-size: ${theme.fonts.size.large};

  &:hover {
    text-decoration: none;
  }
`);
const ToggleActionButton = ({ onClick, isOpen }: Props) => (
  <StyledButton bsStyle="link"
                onClick={() => onClick()}
                type="button">{isOpen ? 'Close' : 'Open'}
    <Icon name={isOpen ? 'angle-down' : 'angle-right'}
          fixedWidth />
  </StyledButton>
);

export default ToggleActionButton;
