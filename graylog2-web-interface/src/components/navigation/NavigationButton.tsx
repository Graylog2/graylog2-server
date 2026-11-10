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
import * as React from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { NAV_ITEM_HEIGHT } from 'theme/constants';
import NavItemStateIndicator, { hoverIndicatorStyles } from 'components/common/NavItemStateIndicator';

const StyledButton = styled(Button)(
  ({ theme }) => css`
    padding: 0 15px;
    background: none;
    border: 0;
    min-height: ${NAV_ITEM_HEIGHT};
    color: ${theme.colors.text.primary};

    && {
      overflow: visible;
    }

    .mantine-Button-label {
      overflow: visible;
    }

    &:hover,
    &:focus-visible {
      ${hoverIndicatorStyles(theme)}
      background: transparent;
      color: ${theme.colors.variant.darker.default};
    }
  `,
);

type Props = React.PropsWithChildren<{
  'aria-label': string;
  id?: string;
  onClick: () => void;
}>;

const NavigationButton = ({ children = undefined, ...props }: Props) => (
  <StyledButton bsStyle="link" type="button" {...props}>
    <NavItemStateIndicator>{children}</NavItemStateIndicator>
  </StyledButton>
);

export default NavigationButton;
