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
import { Spoiler as MantineSpoiler } from '@mantine/core';
import styled, { css } from 'styled-components';

const StyledSpoiler = styled(MantineSpoiler)(
  ({ theme }) => css`
    .mantine-Spoiler-control {
      color: ${theme.colors.link.default};
    }
  `,
);

type Props = React.PropsWithChildren<{
  maxHeight: number;
  className?: string;
  showLabel?: string;
  hideLabel?: string;
}>;

const Spoiler = ({
  maxHeight,
  children = undefined,
  className = undefined,
  showLabel = 'Show more',
  hideLabel = 'Hide',
}: Props) => (
  <StyledSpoiler maxHeight={maxHeight} showLabel={showLabel} hideLabel={hideLabel} className={className}>
    {children}
  </StyledSpoiler>
);

export default Spoiler;
