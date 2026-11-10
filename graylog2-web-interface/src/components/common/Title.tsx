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
import { Title as MantineTitle } from '@mantine/core';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

type Order = 1 | 2 | 3 | 4 | 5 | 6;

const fontSizeForOrder = (order: Order, theme: DefaultTheme) => {
  const sizes: Record<Order, string> = {
    1: theme.fonts.size.h1,
    2: theme.fonts.size.h2,
    3: theme.fonts.size.h3,
    4: theme.fonts.size.h4,
    5: theme.fonts.size.h5,
    6: theme.fonts.size.h6,
  };

  return sizes[order];
};

const StyledTitle = styled(MantineTitle)<{ order: Order }>(
  ({ theme, order }) => css`
    font-size: ${fontSizeForOrder(order, theme)};
    font-family: ${order <= 2 ? theme.fonts.family.navigation : 'inherit'};
    font-weight: ${order === 6 ? 'bold' : 'normal'};
    color: ${theme.colors.text.primary};
    margin: 0;
    padding: 0;
  `,
);

const Title = ({ order = 1, ...props }: React.ComponentProps<typeof MantineTitle>) => (
  <StyledTitle order={order as Order} {...props} />
);

export default Title;
