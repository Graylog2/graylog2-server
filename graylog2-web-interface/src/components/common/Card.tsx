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
import { Card as MantineCard } from '@mantine/core';

const ContainerWrapper = styled.div(
  ({ theme }) => css`
    .mantine-Card-root {
      background-color: ${theme.colors.cards.background};
      border-color: ${theme.colors.cards.border};

      &:focus {
        outline: 5px auto Highlight;
        outline: 5px auto -webkit-focus-ring-color;
      }
    }
  `,
);

type Props = React.PropsWithChildren<{
  className?: string;
  padding?: 'sm';
  id?: string;
  tabIndex?: number;
}>;

/**
 * Simple card component.
 */
const Card = ({
  children = undefined,
  className = undefined,
  padding = undefined,
  id = undefined,
  tabIndex = undefined,
}: Props) => (
  <ContainerWrapper className={className}>
    <MantineCard shadow="sm" padding={padding} radius="md" withBorder tabIndex={tabIndex} id={id}>
      {children}
    </MantineCard>
  </ContainerWrapper>
);

export default Card;
