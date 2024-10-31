/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
    <Icon name={isOpen ? 'keyboard_arrow_down' : 'chevron_right'} />
  </StyledButton>
);

export default ToggleActionButton;
